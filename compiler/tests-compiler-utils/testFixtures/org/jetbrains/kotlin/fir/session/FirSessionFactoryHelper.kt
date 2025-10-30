/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

@ObsoleteTestInfrastructure
object FirSessionFactoryHelper {
    inline fun createSessionWithDependencies(
        moduleName: Name,
        platform: TargetPlatform,
        projectEnvironment: VfsBasedProjectEnvironment,
        configuration: CompilerConfiguration,
        javaSourcesScope: AbstractProjectFileSearchScope,
        librariesScope: AbstractProjectFileSearchScope,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        needRegisterJavaElementFinder: Boolean,
        dependenciesConfigurator: DependencyListForCliModule.Builder.BuilderForDefaultDependenciesModule.() -> Unit = {},
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit = {},
    ): FirSession {
        val dependencyList = DependencyListForCliModule.build(moduleName, init = dependenciesConfigurator)
        val languageVersionSettings = configuration.languageVersionSettings

        val context = FirJvmSessionFactory.Context(
            configuration,
            projectEnvironment,
            librariesScope,
        )

        val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
            moduleName,
            extensionRegistrars,
            languageVersionSettings,
            context,
        )

        val librarySession = FirJvmSessionFactory.createLibrarySession(
            sharedLibrarySession,
            dependencyList.moduleDataProvider,
            extensionRegistrars,
            languageVersionSettings,
            context,
        )

        val mainModuleData = FirSourceModuleData(
            moduleName,
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendDependencies,
            platform,
        )
        return FirJvmSessionFactory.createSourceSession(
            mainModuleData,
            javaSourcesScope,
            { incrementalCompilationContext?.createSymbolProviders(it, mainModuleData, projectEnvironment) },
            extensionRegistrars,
            configuration,
            context,
            needRegisterJavaElementFinder,
            isForLeafHmppModule = false,
        ) {
            registerComponent(FirBuiltinSyntheticFunctionInterfaceProvider::class, librarySession.syntheticFunctionInterfacesSymbolProvider)
            sessionConfigurator()
        }
    }

    @OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
    fun createEmptySession(features: Map<LanguageFeature, LanguageFeature.State> = emptyMap()): FirSession {
        return object : FirSession(Kind.Source) {}.apply {
            val moduleData = FirSourceModuleData(
                Name.identifier("<stub module>"),
                dependencies = emptyList(),
                dependsOnDependencies = emptyList(),
                friendDependencies = emptyList(),
                platform = JvmPlatforms.unspecifiedJvmPlatform,
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
                        return features.getOrDefault(feature, LanguageFeature.State.DISABLED)
                    }

                    override fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State> = stub()

                    override fun isPreRelease(): Boolean = stub()

                    override fun <T> getFlag(flag: AnalysisFlag<T>): T = flag.defaultValue

                    override val apiVersion: ApiVersion
                        get() = stub()
                    override val languageVersion: LanguageVersion
                        get() = stub()
                }
            ))

            register(FirExtensionService::class, FirExtensionService(this))
        }
    }
}
