/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.FirDefaultOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.fir.scopes.impl.FirDelegatedMembersFilter
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

object FirSessionFactoryHelper {
    inline fun createSessionWithDependencies(
        moduleName: Name,
        platform: TargetPlatform,
        analyzerServices: PlatformDependentAnalyzerServices,
        externalSessionProvider: FirProjectSessionProvider?,
        projectEnvironment: AbstractProjectEnvironment,
        languageVersionSettings: LanguageVersionSettings,
        javaSourcesScope: AbstractProjectFileSearchScope,
        librariesScope: AbstractProjectFileSearchScope,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        needRegisterJavaElementFinder: Boolean,
        dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {},
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit = {},
    ): FirSession {
        val binaryModuleData = BinaryModuleData.initialize(moduleName, platform, analyzerServices)
        val dependencyList = DependencyListForCliModule.build(binaryModuleData, init = dependenciesConfigurator)
        val sessionProvider = externalSessionProvider ?: FirProjectSessionProvider()
        val packagePartProvider = projectEnvironment.getPackagePartProvider(librariesScope)
        FirJvmSessionFactory.createLibrarySession(
            moduleName,
            sessionProvider,
            dependencyList.moduleDataProvider,
            projectEnvironment,
            extensionRegistrars,
            librariesScope,
            packagePartProvider,
            languageVersionSettings,
            registerExtraComponents = {},
        )

        val mainModuleData = FirModuleDataImpl(
            moduleName,
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendsDependencies,
            platform,
            analyzerServices
        )
        return FirJvmSessionFactory.createModuleBasedSession(
            mainModuleData,
            sessionProvider,
            javaSourcesScope,
            projectEnvironment,
            { incrementalCompilationContext?.createSymbolProviders(it, mainModuleData, projectEnvironment) },
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            needRegisterJavaElementFinder = needRegisterJavaElementFinder,
            registerExtraComponents = {},
            init = sessionConfigurator,
        )
    }

    @OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
    @TestOnly
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
            // Empty stub for tests
            register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(
                object : LanguageVersionSettings {

                    private fun stub(): Nothing = TODO(
                        "It does not yet have well-defined semantics for tests." +
                                "If you're seeing this, implement it in a test-specific way"
                    )

                    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
                        return LanguageFeature.State.DISABLED
                    }

                    override fun isPreRelease(): Boolean = stub()

                    override fun <T> getFlag(flag: AnalysisFlag<T>): T = stub()

                    override val apiVersion: ApiVersion
                        get() = stub()
                    override val languageVersion: LanguageVersion
                        get() = stub()
                }
            ))

            register(FirExtensionService::class, FirExtensionService(this))
        }
    }

    /**
     * Registers default components for [FirSession]
     * They could be overridden by calling a function that registers specific platform components
     */
    @OptIn(SessionConfiguration::class)
    fun FirSession.registerDefaultComponents() {
        register(FirVisibilityChecker::class, FirVisibilityChecker.Default)
        register(ConeCallConflictResolverFactory::class, DefaultCallConflictResolverFactory)
        register(FirPlatformClassMapper::class, FirPlatformClassMapper.Default)
        register(FirOverridesBackwardCompatibilityHelper::class, FirDefaultOverridesBackwardCompatibilityHelper)
        register(FirDelegatedMembersFilter::class, FirDelegatedMembersFilter.Default)
        register(FirPlatformSpecificCastChecker::class, FirPlatformSpecificCastChecker.Default)
    }
}
