/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.CollectionFactory
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
import org.jetbrains.kotlin.analysis.utils.trackers.CompositeModificationTracker
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
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.partitionIsInstance
import java.util.concurrent.ConcurrentMap

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal class LLFirSessionCache(private val project: Project) {
    companion object {
        fun getInstance(project: Project): LLFirSessionCache {
            return project.getService(LLFirSessionCache::class.java)
        }
    }

    private val globalResolveComponents: LLFirGlobalResolveComponents
        get() = LLFirGlobalResolveComponents.getInstance(project)

    private val sourceCache: ConcurrentMap<KtModule, CachedValue<LLFirSession>> = CollectionFactory.createConcurrentSoftValueMap()
    private val binaryCache: ConcurrentMap<KtModule, CachedValue<LLFirSession>> = CollectionFactory.createConcurrentSoftValueMap()

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     */
    fun getSession(module: KtModule, preferBinary: Boolean = false): LLFirSession {
        if (module is KtBinaryModule && (preferBinary || module is KtSdkModule)) {
            return getCachedSession(module, binaryCache, ::createBinaryLibrarySession)
        }

        return getCachedSession(module, sourceCache, ::createSession)
    }

    /**
     * Returns a session without caching it.
     * Note that session dependencies are still cached.
     */
    internal fun getSessionNoCaching(module: KtModule): LLFirSession {
        return createSession(module)
    }

    private fun <T : KtModule> getCachedSession(
        module: T,
        storage: ConcurrentMap<KtModule, CachedValue<LLFirSession>>,
        factory: (T) -> LLFirSession
    ): LLFirSession {
        checkCanceled()

        return storage.computeIfAbsent(module) {
            CachedValuesManager.getManager(project).createCachedValue {
                val session = factory(module)
                CachedValueProvider.Result(session, session.modificationTracker)
            }
        }.value
    }

    private fun createSession(module: KtModule): LLFirSession {
        return when (module) {
            is KtSourceModule -> createSourcesSession(module)
            is KtLibraryModule, is KtLibrarySourceModule -> createLibrarySession(module)
            is KtSdkModule -> createBinaryLibrarySession(module)
            is KtScriptModule -> createScriptSession(module)
            is KtNotUnderContentRootModule -> createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    private fun createSourcesSession(module: KtSourceModule): LLFirSourcesSession {
        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)

        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val dependencies = collectSourceModuleDependencies(module)
        val dependencyTracker = createSourceModuleDependencyTracker(module, dependencies)
        val session = LLFirSourcesSession(module, dependencyTracker, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val contentScope = module.contentScope

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

            val switchableExtensionDeclarationsSymbolProvider =
                FirSwitchableExtensionDeclarationsSymbolProvider.createIfNeeded(session)?.also {
                    register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it)
                }

            val dependencyProvider = LLFirDependenciesSymbolProvider(this, buildList {
                addDependencySymbolProvidersTo(session, dependencies, this)
                add(builtinsSession.symbolProvider)
            })

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            val syntheticFunctionInterfaceProvider =
                FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(this, moduleData, scopeProvider)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                        switchableExtensionDeclarationsSymbolProvider,
                        javaSymbolProvider,
                        syntheticFunctionInterfaceProvider,
                    ),
                    dependencyProvider,
                )
            )
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            LLFirSessionConfigurator.configure(this)

            extensionService.additionalCheckers.forEach(session.checkersComponent::register)
        }
    }

    private fun createLibrarySession(module: KtModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        val libraryModule = when (module) {
            is KtLibraryModule -> module
            is KtLibrarySourceModule -> module.binaryLibrary
            else -> error("Unexpected module ${module::class.simpleName}")
        }

        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

        val scopeProvider = FirKotlinScopeProvider()
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val dependencyTracker = builtinsSession.modificationTracker
        val session = LLFirLibraryOrLibrarySourceResolvableModuleSession(module, dependencyTracker, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val contentScope = module.contentScope

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
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this, annotationsResolver))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)

            val dependencyProvider = LLFirDependenciesSymbolProvider(this, buildList {
                add(builtinsSession.symbolProvider)

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
            })

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        javaSymbolProvider,
                    ),
                    dependencyProvider,
                )
            )
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))

            LLFirSessionConfigurator.configure(this)
        }
    }

    private fun createBinaryLibrarySession(module: KtBinaryModule): LLFirLibrarySession {
        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)

        val dependencyTracker = ModificationTracker.NEVER_CHANGED
        val session = LLFirLibrarySession(module, dependencyTracker, builtinsSession.builtinTypes)

        val moduleData = createModuleData(session)

        return session.apply {
            registerModuleData(moduleData)
            registerIdeComponents(project)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(LanguageVersionSettingsImpl.DEFAULT/*TODO*/)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = LLFirLibraryProviderFactory.createLibraryProvidersForScope(
                this,
                moduleData,
                kotlinScopeProvider,
                project,
                builtinTypes,
                module.contentScope,
                builtinsSession.symbolProvider
            )

            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))
            register(FirProvider::class, LLFirLibrarySessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            LLFirSessionConfigurator.configure(this)
        }
    }

    private fun createScriptSession(module: KtScriptModule): LLFirScriptSession {
        val platform = module.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)
        val contentScope = module.contentScope

        val dependencies = collectSourceModuleDependencies(module)
        val dependencyTracker = createSourceModuleDependencyTracker(module, dependencies)

        val session = LLFirScriptSession(module, dependencyTracker, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
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

            val dependencyProvider = LLFirDependenciesSymbolProvider(this, buildList {
                addDependencySymbolProvidersTo(session, dependencies, this)
                add(builtinsSession.symbolProvider)
            })

            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, contentScope)
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        javaSymbolProvider,
                        provider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)

            LLFirSessionConfigurator.configure(this)
        }
    }

    private fun createNotUnderContentRootResolvableSession(module: KtNotUnderContentRootModule): LLFirNonUnderContentRootResolvableModuleSession {
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val dependencyTracker = builtinsSession.modificationTracker
        val session = LLFirNonUnderContentRootResolvableModuleSession(module, dependencyTracker, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)

        return session.apply {
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
                project.createPackageProvider(module.contentScope),
                canContainKotlinPackage = true,
            )

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependenciesSymbolProvider(this, listOf(builtinsSession.symbolProvider))

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)

            LLFirSessionConfigurator.configure(this)
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

    private fun collectSourceModuleDependencies(module: KtModule): List<LLFirSession> {
        fun getOrCreateSessionForDependency(dependency: KtModule): LLFirSession? = when (dependency) {
            is KtBuiltinsModule -> null // Built-ins are already added
            is KtBinaryModule -> getSession(dependency, preferBinary = true)
            is KtSourceModule -> getSession(dependency)

            is KtScriptModule,
            is KtScriptDependencyModule,
            is KtNotUnderContentRootModule,
            is KtLibrarySourceModule -> error("Module $module cannot depend on ${dependency::class}: $dependency")
        }

        val dependencyModules = buildSet {
            addAll(module.directRegularDependencies)

            // The dependency provider needs to have access to all direct and indirect `dependsOn` dependencies, as `dependsOn`
            // dependencies are transitive.
            addAll(module.transitiveDependsOnDependencies)
        }

        return dependencyModules.mapNotNull(::getOrCreateSessionForDependency)
    }

    private fun createSourceModuleDependencyTracker(module: KtModule, exposedDependencies: List<LLFirSession>): ModificationTracker {
        val friendDependencies = module.directFriendDependencies
        val trackers = ArrayList<ModificationTracker>(exposedDependencies.size + friendDependencies.size)

        exposedDependencies.forEach { trackers += it.modificationTracker }
        friendDependencies.forEach { trackers += getSession(it).modificationTracker }

        return CompositeModificationTracker.createFlattened(trackers)
    }

    private fun createModuleData(session: LLFirSession): LLFirModuleData {
        return LLFirModuleData(session.ktModule).apply { bindSession(session) }
    }

    /**
     * Adds dependency symbol providers from [dependencies] to [destination]. The function might combine, reorder, or exclude specific
     * symbol providers for optimization.
     */
    private fun addDependencySymbolProvidersTo(
        session: LLFirSession,
        dependencies: List<LLFirSession>,
        destination: MutableList<FirSymbolProvider>,
    ) {
        val dependencyProviders = buildList {
            dependencies.forEach { session ->
                when (val dependencyProvider = session.symbolProvider) {
                    is LLFirModuleWithDependenciesSymbolProvider -> addAll(dependencyProvider.providers)
                    else -> add(dependencyProvider)
                }
            }
        }

        dependencyProviders.mergeDependencySymbolProvidersInto(session, destination)
    }

    /**
     * Merges dependency symbol providers of the same kind if possible. The merged symbol provider usually delegates to its subordinate
     * symbol providers to preserve session semantics, but it will have some form of advantage over individual symbol providers (such as
     * querying an index once instead of N times).
     *
     * [session] should be the session of the dependent module. Because all symbol providers are tied to a session, we need a session to
     * create a combined symbol provider.
     */
    private fun List<FirSymbolProvider>.mergeDependencySymbolProvidersInto(
        session: FirSession,
        destination: MutableList<FirSymbolProvider>,
    ) {
        SymbolProviderMerger(this, destination).apply {
            merge<LLFirProvider.SymbolProvider> { LLFirCombinedKotlinSymbolProvider.merge(session, project, it) }
            merge<FirExtensionSyntheticFunctionInterfaceProvider> { LLFirCombinedSyntheticFunctionSymbolProvider.merge(session, it) }
            finish()
        }
    }

    private class SymbolProviderMerger(
        symbolProviders: List<FirSymbolProvider>,
        private val destination: MutableList<FirSymbolProvider>
    ) {
        private var remainingSymbolProviders = symbolProviders

        inline fun <reified A : FirSymbolProvider> merge(create: (List<A>) -> FirSymbolProvider?) {
            val (specificSymbolProviders, remainingSymbolProviders) = remainingSymbolProviders.partitionIsInstance<_, A>()
            destination.addIfNotNull(create(specificSymbolProviders))
            this.remainingSymbolProviders = remainingSymbolProviders
        }

        fun finish() {
            destination.addAll(remainingSymbolProviders)
            remainingSymbolProviders = emptyList()
        }
    }
}

internal fun LLFirSessionConfigurator.Companion.configure(session: LLFirSession) {
    val project = session.project
    for (extension in extensionPointName.getExtensionList(project)) {
        extension.configure(session)
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
