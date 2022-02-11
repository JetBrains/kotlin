/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.IdeSessionComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createPackagePartProviderForLibrary
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object LLFirSessionFactory {
    fun createSourcesSession(
        project: Project,
        module: KtSourceModule,
        builtinsAndCloneableSession: LLFirBuiltinsAndCloneableSession,
        firPhaseRunner: LLFirPhaseRunner,
        sessionInvalidator: LLFirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<KtModule, LLFirResolvableModuleSession>,
        isRootModule: Boolean,
        librariesCache: LibrariesCache,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirSourcesSession {
        sessionsCache[module]?.let { return it as LLFirSourcesSession }
        val languageVersionSettings = module.languageVersionSettings
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val contentScope = module.contentScope
        val dependentModules = module.directRegularDependenciesOfType<KtSourceModule>()
        val session = LLFirSourcesSession(module, project, firBuilder, builtinTypes)
        sessionsCache[module] = session

        return session.apply session@{
            val moduleData = KtModuleBasedModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            val cache = ModuleFileCacheImpl(this)
            val firPhaseManager = LLFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache, sessionInvalidator)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                project,
                this,
                module,
                scopeProvider,
                firFileBuilder,
                cache,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
            )

            register(FirProvider::class, provider)
            register(LLFirProvider::class, provider)

            register(FirPhaseManager::class, firPhaseManager)

            @OptIn(ExperimentalStdlibApi::class)
            val dependentProviders = buildList {
                add(
                    createModuleLibrariesSession(
                        module,
                        project,
                        builtinsAndCloneableSession,
                        builtinTypes,
                        librariesCache,
                        languageVersionSettings = languageVersionSettings,
                        configureSession = configureSession,
                    ).symbolProvider
                )
                dependentModules
                    .mapTo(this) {
                        createSourcesSession(
                            project,
                            it,
                            builtinsAndCloneableSession,
                            firPhaseRunner,
                            sessionInvalidator,
                            builtinTypes,
                            sessionsCache,
                            isRootModule = false,
                            librariesCache = librariesCache,
                            configureSession = configureSession,
                        ).symbolProvider
                    }
            }

            FirSessionFactory.FirSessionConfigurator(this).apply {
                if (isRootModule) {
                    registerExtendedCommonCheckers()
                }
                for (extensionRegistrar in FirExtensionRegistrar.getInstances(project)) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()

            val dependencyProvider = DependentModuleProviders(this, dependentProviders)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        JavaSymbolProvider(
                            this,
                            FirJavaFacade(
                                this, moduleData, project.createJavaClassFinder(contentScope)
                            )
                        ),
                    ),
                    dependencyProvider
                )
            )

            register(FirDependenciesSymbolProvider::class, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            configureSession?.invoke(this)
        }
    }

    private fun createModuleLibrariesSession(
        sourceModule: KtSourceModule,
        project: Project,
        builtinsAndCloneableSession: LLFirBuiltinsAndCloneableSession,
        builtinTypes: BuiltinTypes,
        librariesCache: LibrariesCache,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (LLFirSession.() -> Unit)?,
    ): LLFirLibrariesSession = librariesCache.cached(sourceModule) {
        checkCanceled()
        val searchScope = project.moduleScopeProvider.getModuleLibrariesScope(sourceModule)
        LLFirLibrariesSession(project, builtinTypes).apply session@{
            registerModuleData(KtModuleBasedModuleData(sourceModule).apply { bindSession(this@session) })
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val mainModuleData = KtModuleBasedModuleData(sourceModule).apply { bindSession(this@session) }

            val classFileBasedSymbolProvider = JvmClassFileBasedSymbolProvider(
                this@session,
                moduleDataProvider = createModuleDataProvider(sourceModule, this),
                kotlinScopeProvider = kotlinScopeProvider,
                packagePartProvider = project.createPackagePartProviderForLibrary(searchScope),
                kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope),
                javaFacade = FirJavaFacade(
                    this@session, mainModuleData, project.createJavaClassFinder(searchScope)
                )
            )
            val symbolProvider =
                FirCompositeSymbolProvider(this, listOf(classFileBasedSymbolProvider, builtinsAndCloneableSession.symbolProvider))
            register(FirProvider::class, LLFirLibrariesSessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    private fun createModuleDataProvider(sourceModule: KtModule, session: LLFirSession): ModuleDataProvider {
        val dependencyList = DependencyListForCliModule.build(
            Name.special("<${sourceModule.moduleDescription}>"),
            sourceModule.platform,
            sourceModule.analyzerServices
        ) {
            dependencies(sourceModule.directRegularDependencies.extractLibraryPaths())
            friendDependencies(sourceModule.directFriendDependencies.extractLibraryPaths())
            dependsOnDependencies(sourceModule.directRefinementDependencies.extractLibraryPaths())
        }
        return dependencyList.moduleDataProvider.apply {
            allModuleData.forEach { it.bindSession(session) }
        }
    }

    fun createBuiltinsAndCloneableSession(
        project: Project,
        builtinTypes: BuiltinTypes,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (LLFirSession.() -> Unit)? = null,
    ): LLFirBuiltinsAndCloneableSession {
        return LLFirBuiltinsAndCloneableSession(project, builtinTypes).apply session@{
            val moduleData = FirModuleDataImpl(
                Name.special("<builtins module>"),
                emptyList(),
                emptyList(),
                emptyList(),
                JvmPlatforms.unspecifiedJvmPlatform,
                JvmPlatformAnalyzerServices
            ).apply {
                bindSession(this@session)
            }
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerModuleData(moduleData)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)
            val symbolProvider = FirCompositeSymbolProvider(
                this,
                listOf(
                    LLFirBuiltinSymbolProvider(this, moduleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(this, moduleData, kotlinScopeProvider),
                )
            )
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, LLFirBuiltinsAndCloneableSessionProvider(symbolProvider))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    fun createLibraryOrLibrarySourceResolvableSession(
        project: Project,
        module: KtModule,
        builtinsAndCloneableSession: LLFirBuiltinsAndCloneableSession,
        firPhaseRunner: LLFirPhaseRunner,
        sessionInvalidator: LLFirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<KtModule, LLFirResolvableModuleSession>,
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
        val firFileBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val contentScope = module.contentScope
        val session = LLFirLibraryOrLibrarySourceResolvableModuleSession(module, project, firFileBuilder, builtinTypes)
        sessionsCache[module] = session

        return session.apply session@{
            val moduleData = KtModuleBasedModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            val cache = ModuleFileCacheImpl(this)
            val firPhaseManager = LLFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache, sessionInvalidator)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                project,
                this,
                module,
                scopeProvider,
                firFileBuilder,
                cache,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
            )

            register(FirProvider::class, provider)
            register(LLFirProvider::class, provider)

            register(FirPhaseManager::class, firPhaseManager)
            val dependentProviders = buildList {
                val librariesSearchScope = ProjectScope.getLibrariesScope(project)
                    .intersectWith(GlobalSearchScope.notScope(libraryModule.contentScope)) // <all libraries scope> - <current library scope>
                add(builtinsAndCloneableSession.symbolProvider)
                add(
                    JvmClassFileBasedSymbolProvider(
                        this@session,
                        moduleDataProvider = createModuleDataProvider(module, this@session),
                        kotlinScopeProvider = scopeProvider,
                        packagePartProvider = project.createPackagePartProviderForLibrary(librariesSearchScope),
                        kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(librariesSearchScope),
                        javaFacade = FirJavaFacade(
                            this@session, moduleData, project.createJavaClassFinder(librariesSearchScope)
                        )
                    )
                )
            }

            FirSessionFactory.FirSessionConfigurator(this).apply {
                for (extensionRegistrar in FirExtensionRegistrar.getInstances(project)) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()

            val dependencyProvider = DependentModuleProviders(this, dependentProviders)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        JavaSymbolProvider(
                            this,
                            FirJavaFacade(
                                this, moduleData, project.createJavaClassFinder(contentScope)
                            )
                        ),
                    ),
                    dependencyProvider
                )
            )

            register(FirDependenciesSymbolProvider::class, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            configureSession?.invoke(this)
        }

    }

    private fun LLFirSession.registerIdeComponents(project: Project) {
        register(IdeSessionComponents::class, IdeSessionComponents.create(this))
        register(FirCachesFactory::class, FirThreadSafeCachesFactory)
        register(SealedClassInheritorsProvider::class, project.createSealedInheritorsProvider())
    }
}

private fun List<KtModule>.extractLibraryPaths(): List<Path> =
    asSequence()
        .filterIsInstance<KtBinaryModule>()
        .flatMap { it.getBinaryRoots() }
        .toList()

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
