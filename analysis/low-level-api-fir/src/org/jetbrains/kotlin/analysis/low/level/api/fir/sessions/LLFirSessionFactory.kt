/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.IdeSessionComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createPackagePartProviderForLibrary
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirKtModuleBasedModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createAnnotationResolver
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.utils.errors.checkIsInstance
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.EmptyModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
        globalResolveComponents: LLFirGlobalResolveComponents,
        sessionInvalidator: LLFirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<KtModule, LLFirResolvableModuleSession>,
        isRootModule: Boolean,
        librariesCache: LibrariesCache,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirSourcesSession {
        sessionsCache[module]?.let { return it as LLFirSourcesSession }
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

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val contentScope = module.contentScope
        val dependentModules = module.directRegularDependenciesOfType<KtSourceModule>()
        val session = LLFirSourcesSession(module, project, components, builtinTypes)
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirKtModuleBasedModuleData(module).apply { bindSession(this@session) }
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
            )

            register(FirProvider::class, provider)
            register(LLFirProvider::class, provider)

            register(FirPhaseManager::class, LLFirPhaseManager(sessionInvalidator))

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
                            globalResolveComponents,
                            sessionInvalidator,
                            builtinTypes,
                            sessionsCache,
                            isRootModule = false,
                            librariesCache = librariesCache,
                            configureSession = configureSession,
                        ).symbolProvider
                    }
            }

            val projectWithDependenciesScope = contentScope.uniteWith(project.moduleScopeProvider.getModuleLibrariesScope(module))
            val annotationsResolver = project.createAnnotationResolver(projectWithDependenciesScope)

            // We need FirRegisteredPluginAnnotations and FirPredicateBasedProvider during extensions' registration process
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this@session, annotationsResolver))
            register(
                FirPredicateBasedProvider::class,
                LLFirIdePredicateBasedProvider(
                    this@session,
                    annotationsResolver,
                    project.createDeclarationProvider(projectWithDependenciesScope)
                )
            )

            FirSessionFactory.FirSessionConfigurator(this).apply {
                if (isRootModule) {
                    registerExtendedCommonCheckers()
                }
                for (extensionRegistrar in FirExtensionRegistrar.getInstances(project)) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()

            val switchableExtensionDeclarationsSymbolProvider =
                FirSwitchableExtensionDeclarationsSymbolProvider.create(session)

            switchableExtensionDeclarationsSymbolProvider?.let {
                register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it)
            }

            val dependencyProvider = DependentModuleProviders(this, dependentProviders)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                        switchableExtensionDeclarationsSymbolProvider,
                        JavaSymbolProvider(
                            this,
                            FirJavaFacadeForSource(
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
        LLFirLibrariesSession(project, builtinTypes).apply session@{
            registerModuleData(LLFirKtModuleBasedModuleData(sourceModule).apply { bindSession(this@session) })
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val providers = createProvidersByModulePrimaryDependencies(
                this,
                sourceModule,
                kotlinScopeProvider,
                project,
                builtinTypes
            ) { binaryDependencies ->
                GlobalSearchScope.union(binaryDependencies.map { it.contentScope })
            }


            val symbolProvider = FirCompositeSymbolProvider(
                this,
                buildList {
                    addAll(providers)
                    add(builtinsAndCloneableSession.symbolProvider)
                }
            )
            register(FirProvider::class, LLFirLibrariesSessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    private fun createProvidersByModulePrimaryDependencies(
        session: LLFirSession,
        module: KtModule,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        createSearchScopeForModules: (List<KtBinaryModule>) -> GlobalSearchScope
    ): List<FirSymbolProvider> = buildList {
        module
            .allDirectDependenciesOfType<KtBinaryModule>()
            .groupBy { it.platform }
            .forEach { (_, binaryDependencies) ->
                val moduleDataProvider = createModuleDataProviderWithLibraryDependencies(module, binaryDependencies, session)
                val scope = createSearchScopeForModules(binaryDependencies)
                val packagePartProvider = project.createPackagePartProviderForLibrary(scope)
                add(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        VirtualFileFinderFactory.getInstance(project).create(scope),
                        LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
                    )
                )
                add(OptionalAnnotationClassesProvider(session, moduleDataProvider, kotlinScopeProvider, packagePartProvider))
            }
    }

    private fun createModuleDataProviderWithLibraryDependencies(
        sourceModule: KtModule,
        binaryDependencies: List<KtBinaryModule>,
        session: LLFirSession
    ): ModuleDataProvider {
        val moduleDatas = binaryDependencies.map { LLFirKtModuleBasedModuleData(it) }

        if (moduleDatas.isEmpty()) {
            return EmptyModuleDataProvider(sourceModule.platform, sourceModule.analyzerServices)
        }

        moduleDatas.forEach { it.bindSession(session) }

        val moduleDataWithFilters: Map<FirModuleData, LibraryPathFilter.LibraryList> =
            moduleDatas.associateWith { moduleData ->
                checkIsInstance<LLFirKtModuleBasedModuleData>(moduleData)
                val ktBinaryModule = moduleData.ktModule as KtBinaryModule
                val moduleBinaryRoots = ktBinaryModule.getBinaryRoots().mapTo(mutableSetOf()) { it.toAbsolutePath() }
                LibraryPathFilter.LibraryList(moduleBinaryRoots)
            }

        return MultipleModuleDataProvider(moduleDataWithFilters)
    }

    fun createBuiltinsAndCloneableSession(
        project: Project,
        builtinTypes: BuiltinTypes,
        useSiteModule: KtModule,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (LLFirSession.() -> Unit)? = null,
    ): LLFirBuiltinsAndCloneableSession {
        return LLFirBuiltinsAndCloneableSession(project, builtinTypes).apply session@{
            val moduleData = LLFirBuiltinsModuleData(useSiteModule).apply {
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
        globalComponents: LLFirGlobalResolveComponents,
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
        val components = LLFirModuleResolveComponents(module, globalComponents, scopeProvider)

        val contentScope = module.contentScope
        val session = LLFirLibraryOrLibrarySourceResolvableModuleSession(module, project, components, builtinTypes)
        sessionsCache[module] = session
        components.session = session

        return session.apply session@{
            val moduleData = LLFirKtModuleBasedModuleData(module).apply { bindSession(this@session) }
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
            )

            register(FirProvider::class, provider)
            register(LLFirProvider::class, provider)

            register(FirPhaseManager::class, LLFirPhaseManager(sessionInvalidator))
            val dependentProviders = buildList {
                val librariesSearchScope = ProjectScope.getLibrariesScope(project)
                    .intersectWith(GlobalSearchScope.notScope(libraryModule.contentScope)) // <all libraries scope> - <current library scope>
                add(builtinsAndCloneableSession.symbolProvider)
                addAll(createProvidersByModulePrimaryDependencies(session, module, scopeProvider, project, builtinTypes) { librariesSearchScope })
            }

            // We need FirRegisteredPluginAnnotations during extensions' registration process
            val annotationsResolver = project.createAnnotationResolver(contentScope)
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this@session, annotationsResolver))
            register(
                FirPredicateBasedProvider::class,
                object : FirPredicateBasedProvider() {
                    override fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirBasedSymbol<*>> = emptyList()

                    override fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>? = null

                    override fun fileHasPluginAnnotations(file: FirFile): Boolean = false

                    override fun matches(predicate: DeclarationPredicate, declaration: FirDeclaration): Boolean = false
                }
            )

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
                            FirJavaFacadeForSource(
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
        .map { it.toAbsolutePath() }
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
