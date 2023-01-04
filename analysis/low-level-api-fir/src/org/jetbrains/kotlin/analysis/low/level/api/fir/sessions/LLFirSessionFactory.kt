/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createAnnotationResolver
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object LLFirSessionFactory {
    fun createSourcesSession(
        project: Project,
        module: KtSourceModule,
        globalResolveComponents: LLFirGlobalResolveComponents,
        sessionInvalidator: LLFirSessionInvalidator,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
        librariesSessionFactory: LLFirLibrarySessionFactory,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirSourcesSession {
        sessionsCache[module]?.let { return it as LLFirSourcesSession }
        checkCanceled()

        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)

        val languageVersionSettings = object : LanguageVersionSettings by module.languageVersionSettings {
            override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                if (feature == LanguageFeature.EnableDfaWarningsInK2) LanguageFeature.State.ENABLED
                else module.languageVersionSettings.getFeatureSupport(feature)

            override fun supportsFeature(feature: LanguageFeature): Boolean =
                getFeatureSupport(feature).let {
                    it == LanguageFeature.State.ENABLED || it == LanguageFeature.State.ENABLED_WITH_WARNING
                }
        }

        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider, sessionInvalidator)

        val contentScope = module.contentScope
        val session = LLFirSourcesSession(
            module,
            project,
            components,
            builtinsSession.builtinTypes
        )
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                this,
                components,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
                /* Source modules can contain `kotlin` package only if `-Xallow-kotlin-package` is specified, this is handled in LLFirProvider */
                canContainKotlinPackage = false,
            )

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            registerCompilerPluginServices(contentScope, project, module)
            registerCompilerPluginExtensions(project, module)

            val switchableExtensionDeclarationsSymbolProvider = FirSwitchableExtensionDeclarationsSymbolProvider.create(session)?.also {
                register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it)
            }

            val dependencyProvider = LLFirDependentModuleProvidersBySessions(this) {
                module.directRegularDependencies.mapNotNullTo(this) { dependency ->
                    when (dependency) {
                        is KtBuiltinsModule -> null //  build in is already added
                        is KtBinaryModule -> LLFirLibrarySessionFactory.getInstance(project).getLibrarySession(dependency, sessionsCache)
                        is KtSourceModule -> {
                            createSourcesSession(
                                project,
                                dependency,
                                globalResolveComponents,
                                sessionInvalidator,
                                sessionsCache,
                                librariesSessionFactory = librariesSessionFactory,
                                configureSession = configureSession,
                            )
                        }
                        is KtNotUnderContentRootModule -> error("Module $module cannot depend on ${dependency::class}: $dependency")
                        is KtLibrarySourceModule -> error("Module $module cannot depend on ${dependency::class}: $dependency")
                    }
                }
                add(builtinsSession)
            }

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                        switchableExtensionDeclarationsSymbolProvider,
                        javaSymbolProvider,
                    ),
                )
            )
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            configureSession?.invoke(this)
            extensionService.additionalCheckers.forEach(session.checkersComponent::register)
        }
    }


    fun createLibraryOrLibrarySourceResolvableSession(
        project: Project,
        module: KtModule,
        globalComponents: LLFirGlobalResolveComponents,
        sessionInvalidator: LLFirSessionInvalidator,
        builtinSession: LLFirBuiltinsAndCloneableSession,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        LLFirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidKtModule(module)
        sessionsCache[module]?.let { return it as LLFirLibraryOrLibrarySourceResolvableModuleSession }
        checkCanceled()

        val libraryModule = when (module) {
            is KtLibraryModule -> module
            is KtLibrarySourceModule -> module.binaryLibrary
            else -> error("Unexpected module ${module::class.simpleName}")
        }

        val scopeProvider = FirKotlinScopeProvider()
        val components = LLFirModuleResolveComponents(module, globalComponents, scopeProvider, sessionInvalidator)

        val contentScope = module.contentScope
        val session = LLFirLibraryOrLibrarySourceResolvableModuleSession(module, project, components, builtinSession.builtinTypes)
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                this,
                components,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
                canContainKotlinPackage = true,
            )

            register(FirProvider::class, provider)

            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            // We need FirRegisteredPluginAnnotations during extensions' registration process
            val annotationsResolver = project.createAnnotationResolver(contentScope)
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this@session, annotationsResolver))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)

            val dependencyProvider = LLFirDependentModuleProvidersByProviders(this) {
                // <all libraries scope> - <current library scope>
                val librariesSearchScope =
                    ProjectScope.getLibrariesScope(project).intersectWith(GlobalSearchScope.notScope(libraryModule.contentScope))
                add(builtinSession.symbolProvider)
                addAll(
                    LLFirLibraryProviderFactory.createLibraryProvidersForAllProjectLibraries(
                        session, moduleData, scopeProvider, project, builtinTypes, librariesSearchScope
                    )
                )
            }

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOf(
                        provider.symbolProvider,
                        javaSymbolProvider,
                    ),
                )
            )
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            configureSession?.invoke(this)
        }

    }
}

@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
fun createEmptySession(): FirSession {
    return object : FirSession(null, Kind.Source) {}.apply {
        val moduleData = FirModuleDataImpl(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
            analyzerServices = JvmPlatformAnalyzerServices
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
