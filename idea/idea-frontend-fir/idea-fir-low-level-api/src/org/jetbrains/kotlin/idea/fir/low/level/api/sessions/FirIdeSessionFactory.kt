/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeFirPhaseManager
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveStateConfigurator
import org.jetbrains.kotlin.idea.fir.low.level.api.api.stateConfigurator
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.*
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeBuiltinsAndCloneableSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeLibrariesSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object FirIdeSessionFactory {
    fun createSourcesSession(
        project: Project,
        configurator: FirModuleResolveStateConfigurator,
        moduleInfo: ModuleSourceInfoBase,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        firPhaseRunner: FirPhaseRunner,
        sessionInvalidator: FirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<ModuleSourceInfoBase, FirIdeSourcesSession>,
        isRootModule: Boolean,
        librariesCache: LibrariesCache,
        configureSession: (FirIdeSession.() -> Unit)? = null
    ): FirIdeSourcesSession {
        sessionsCache[moduleInfo]?.let { return it }
        val languageVersionSettings = project.stateConfigurator.getLanguageVersionSettings(moduleInfo)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val searchScope = project.stateConfigurator.getModuleSourceScope(moduleInfo)
        val dependentModules = moduleInfo.dependenciesWithoutSelf()
            .filterIsInstanceTo<ModuleSourceInfoBase, MutableList<ModuleSourceInfoBase>>(mutableListOf())
        val session = FirIdeSourcesSession(dependentModules, project, searchScope, firBuilder, builtinTypes)
        sessionsCache[moduleInfo] = session

        return session.apply session@{
            val moduleData = FirModuleInfoBasedModuleData(moduleInfo).apply { bindSession(this@session) }
            registerModuleData(moduleData)

            val cache = ModuleFileCacheImpl(this)
            val firPhaseManager = IdeFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache, sessionInvalidator)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents()
            registerResolveComponents()

            val provider = FirIdeProvider(
                project,
                this,
                moduleInfo,
                scopeProvider,
                firFileBuilder,
                cache,
                project.createDeclarationProvider(searchScope),
                project.createPackageProvider(searchScope),
            )

            register(FirProvider::class, provider)
            register(FirIdeProvider::class, provider)

            register(FirPhaseManager::class, firPhaseManager)

            @OptIn(ExperimentalStdlibApi::class)
            val dependentProviders = buildList {
                add(
                    createLibrarySession(
                        moduleInfo,
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
                            configurator,
                            it,
                            builtinsAndCloneableSession,
                            firPhaseRunner,
                            sessionInvalidator,
                            builtinTypes,
                            sessionsCache,
                            isRootModule = false,
                            librariesCache,
                            configureSession = configureSession,
                        ).symbolProvider
                    }
            }

            val dependencyProvider = DependentModuleProviders(this, dependentProviders)

            register(
                FirSymbolProvider::class,
                FirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        JavaSymbolProvider(this@session, moduleData, project, searchScope),
                    ),
                    dependencyProvider
                )
            )

            register(FirDependenciesSymbolProvider::class, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))

            registerJavaSpecificResolveComponents()
            FirSessionFactory.FirSessionConfigurator(this).apply {
                if (isRootModule) {
                    registerExtendedCommonCheckers()
                }
            }.configure()
            configureSession?.invoke(this)
            project.stateConfigurator.configureSourceSession(this)
        }
    }

    fun createLibrarySession(
        mainModuleInfo: ModuleSourceInfoBase,
        project: Project,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        builtinTypes: BuiltinTypes,
        librariesCache: LibrariesCache,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (FirIdeSession.() -> Unit)?,
    ): FirIdeLibrariesSession = librariesCache.cached(mainModuleInfo) {
        checkCanceled()
        val searchScope = project.stateConfigurator.createScopeForModuleLibraries(mainModuleInfo)
        val javaClassFinder = JavaClassFinderImpl().apply {
            setProjectInstance(project)
            setScope(searchScope)
        }
        val packagePartProvider = project.stateConfigurator.createPackagePartsProvider(mainModuleInfo, searchScope)

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope)
        FirIdeLibrariesSession(project, searchScope, builtinTypes).apply session@{
            val mainModuleData = FirModuleInfoBasedModuleData(mainModuleInfo).apply { bindSession(this@session) }

            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents()
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

            val moduleDataProvider = project.stateConfigurator.createModuleDataProvider(mainModuleInfo)

            moduleDataProvider.allModuleData.forEach { it.bindSession(this@session) }

            val symbolProvider = FirCompositeSymbolProvider(
                this,
                @OptIn(ExperimentalStdlibApi::class)
                buildList {
                    add(
                        KotlinDeserializedJvmSymbolsProviderForIde(
                            this@session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            packagePartProvider,
                            kotlinClassFinder,
                            javaClassFinder
                        )
                    )
                    add(JavaSymbolProvider(this@session, mainModuleData, project, searchScope))
                    addAll((builtinsAndCloneableSession.symbolProvider as FirCompositeSymbolProvider).providers)
                }
            )
            register(FirProvider::class, FirIdeLibrariesSessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    /**
     * Workaround of SOE for loading classes that loads via parent.
     * It is not observable in compiler but it is in IDE
     * Remove this when cyclic cache request will be fixed in the compiler provider
     */
    private class KotlinDeserializedJvmSymbolsProviderForIde(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        packagePartProvider: PackagePartProvider,
        kotlinClassFinder: KotlinClassFinder,
        javaClassFinder: JavaClassFinder
    ) : KotlinDeserializedJvmSymbolsProvider(
        session, moduleDataProvider, kotlinScopeProvider, packagePartProvider, kotlinClassFinder,
        javaClassFinder
    ) {
        override fun getClass(
            classId: ClassId,
            parentContext: FirDeserializationContext?
        ): FirRegularClassSymbol? {
            if (parentContext !== null) {
                return super.getClass(classId, parentContext)
            }
            val computedClass = classCache.getValueIfComputed(classId)
            if (computedClass != null) {
                return computedClass
            }

            var parentClassId = classId
            while (true) {
                parentClassId = parentClassId.outerClassId ?: break
            }

            classCache.getValue(parentClassId, parentContext)
            return classCache.getValue(classId, parentContext)
        }
    }

    fun createBuiltinsAndCloneableSession(
        project: Project,
        builtinTypes: BuiltinTypes,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        configureSession: (FirIdeSession.() -> Unit)? = null,
    ): FirIdeBuiltinsAndCloneableSession {
        return FirIdeBuiltinsAndCloneableSession(project, builtinTypes).apply session@{
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

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            val symbolProvider = FirCompositeSymbolProvider(
                this,
                listOf(
                    FirIdeBuiltinSymbolProvider(this, moduleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(this, moduleData, kotlinScopeProvider),
                )
            )
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirIdeBuiltinsAndCloneableSessionProvider(symbolProvider))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            configureSession?.invoke(this)
        }
    }

    private fun FirIdeSession.registerIdeComponents(project: Project) {
        register(IdeSessionComponents::class, IdeSessionComponents.create(this))
        register(FirCachesFactory::class, FirThreadSafeCachesFactory)
        register(SealedClassInheritorsProvider::class, project.stateConfigurator.createSealedInheritorsProvider())
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
