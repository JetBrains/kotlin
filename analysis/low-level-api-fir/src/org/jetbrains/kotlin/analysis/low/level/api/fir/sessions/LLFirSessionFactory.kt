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
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
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
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)

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
            registerCommonComponentsAfterExtensionsAreConfigured()

            val switchableExtensionDeclarationsSymbolProvider = FirSwitchableExtensionDeclarationsSymbolProvider.create(session)?.also {
                register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it)
            }

            val dependencyProvider = LLFirDependentModuleProvidersBySessions(this) {
                processSourceDependencies(
                    module,
                    sessionsCache,
                    globalResolveComponents,
                    sessionInvalidator,
                    librariesSessionFactory,
                    configureSession
                )

                add(builtinsSession)
            }

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            val syntheticFunctionalInterfaceProvider = FirExtensionSyntheticFunctionInterfaceProvider(this, moduleData, scopeProvider)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                        switchableExtensionDeclarationsSymbolProvider,
                        javaSymbolProvider,
                        syntheticFunctionalInterfaceProvider,
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
            registerCommonComponentsAfterExtensionsAreConfigured()
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
                add(builtinSession.symbolProvider)

                // Script dependencies are self-contained and should not depend on other libraries
                if (module !is KtScriptDependencyModule) {
                    // Add all libraries excluding the current one
                    val librariesSearchScope = ProjectScope.getLibrariesScope(project)
                        .intersectWith(GlobalSearchScope.notScope(libraryModule.contentScope))

                    val restLibrariesProvider = LLFirLibraryProviderFactory.createLibraryProvidersForAllProjectLibraries(
                        session, moduleData, scopeProvider,
                        project, builtinTypes, librariesSearchScope
                    )

                    addAll(restLibrariesProvider)
                }
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
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            configureSession?.invoke(this)
        }
    }

    fun createScriptSession(
        project: Project,
        module: KtScriptModule,
        sessionInvalidator: LLFirSessionInvalidator,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
        librariesSessionFactory: LLFirLibrarySessionFactory,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirScriptSession {
        sessionsCache[module]?.let { return it as LLFirScriptSession }
        checkCanceled()

        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val globalResolveComponents = LLFirGlobalResolveComponents(project)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider, sessionInvalidator)
        val contentScope = module.contentScope

        val session = LLFirScriptSession(module, project, components, builtinsSession.builtinTypes)
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                this,
                components,
                FileBasedKotlinDeclarationProvider(module.file),
                project.createPackageProvider(contentScope),
                canContainKotlinPackage = true,
            )

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependentModuleProvidersBySessions(this) {
                processSourceDependencies(
                    module,
                    sessionsCache,
                    globalResolveComponents,
                    sessionInvalidator,
                    librariesSessionFactory,
                    configureSession
                )

                add(builtinsSession)
            }

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOfNotNull(
                        javaSymbolProvider,
                        provider.symbolProvider,
                    )
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)

            configureSession?.invoke(this)
        }
    }

    fun createNotUnderContentRootResolvableSession(
        project: Project,
        module: KtNotUnderContentRootModule,
        sessionInvalidator: LLFirSessionInvalidator,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirNonUnderContentRootResolvableModuleSession {
        sessionsCache[module]?.let { return it as LLFirNonUnderContentRootResolvableModuleSession }
        checkCanceled()

        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val globalResolveComponents = LLFirGlobalResolveComponents(project)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider, sessionInvalidator)
        val contentScope = module.contentScope

        val session = LLFirNonUnderContentRootResolvableModuleSession(module, project, components, builtinsSession.builtinTypes)
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val ktFile = module.file as? KtFile

            val provider = LLFirProvider(
                this,
                components,
                if (ktFile != null) FileBasedKotlinDeclarationProvider(ktFile) else EmptyKotlinDeclarationProvider,
                project.createPackageProvider(contentScope),
                canContainKotlinPackage = true,
            )

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependentModuleProvidersBySessions(this) {
                add(builtinsSession)
            }

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                    )
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)

            configureSession?.invoke(this)
        }
    }

    private fun wrapLanguageVersionSettings(original: LanguageVersionSettings): LanguageVersionSettings {
        return object : LanguageVersionSettings by original {
            override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
                return when (feature) {
                    LanguageFeature.EnableDfaWarningsInK2 -> LanguageFeature.State.ENABLED
                    else -> original.getFeatureSupport(feature)
                }
            }

            override fun supportsFeature(feature: LanguageFeature): Boolean {
                return when (getFeatureSupport(feature)) {
                    LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_WARNING -> true
                    else -> false
                }
            }
        }
    }

    private fun MutableList<LLFirSession>.processSourceDependencies(
        module: KtModule,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
        globalResolveComponents: LLFirGlobalResolveComponents,
        sessionInvalidator: LLFirSessionInvalidator,
        librariesSessionFactory: LLFirLibrarySessionFactory,
        configureSession: (LLFirSession.() -> Unit)?
    ) {
        val project = module.project

        fun getOrCreateSessionForDependency(dependency: KtModule): LLFirSession? = when (dependency) {
            is KtBuiltinsModule -> {
                // Built-ins are already added
                null
            }

            is KtBinaryModule -> {
                LLFirLibrarySessionFactory.getInstance(project).getLibrarySession(dependency, sessionsCache)
            }

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

            is KtScriptModule,
            is KtScriptDependencyModule,
            is KtNotUnderContentRootModule,
            is KtLibrarySourceModule -> {
                error("Module $module cannot depend on ${dependency::class}: $dependency")
            }
        }

        module.directRegularDependencies.mapNotNullTo(this@processSourceDependencies, ::getOrCreateSessionForDependency)

        // The dependency provider needs to have access to all direct and indirect `dependsOn` dependencies, as `dependsOn`
        // dependencies are transitive.
        val directRegularDependenciesSet = module.directRegularDependencies.toSet()
        module.transitiveDependsOnDependencies.forEach { dependency ->
            if (dependency !in directRegularDependenciesSet) {
                getOrCreateSessionForDependency(dependency)?.let(this::add)
            }
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
