/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

@OptIn(SessionConfiguration::class)
object FirCommonSessionFactory : FirAbstractSessionFactory<Nothing?, Nothing?>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        librariesScope: AbstractProjectFileSearchScope,
        resolvedKLibs: List<KotlinResolvedLibrary>,
        packageAndMetadataPartProvider: PackageAndMetadataPartProvider,
        languageVersionSettings: LanguageVersionSettings,
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            context = null,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
                listOfNotNull(
                    MetadataSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packageAndMetadataPartProvider,
                        projectEnvironment.getKotlinClassFinder(librariesScope)
                    ),
                    runIf(resolvedKLibs.isNotEmpty()) {
                        KlibBasedSymbolProvider(
                            session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            resolvedKLibs.map { it.library }
                        )
                    },
                    syntheticFunctionInterfaceProvider,
                    runUnless(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                        FirFallbackBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider)
                    },
                    FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                )
            }
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSession.registerLibrarySessionComponents(c: Nothing?) {
        registerDefaultComponents()
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        useExtraCheckers: Boolean,
        lookupTracker: LookupTracker? = null,
        enumWhenTracker: EnumWhenTracker? = null,
        importTracker: ImportTracker? = null,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            context = null,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            useExtraCheckers,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependencies ->
                var symbolProviderForBinariesFromIncrementalCompilation: MetadataSymbolProvider? = null
                incrementalCompilationContext?.let {
                    val precompiledBinariesPackagePartProvider = it.precompiledBinariesPackagePartProvider
                    if (precompiledBinariesPackagePartProvider != null && it.precompiledBinariesFileScope != null) {
                        val moduleDataProvider = SingleModuleDataProvider(moduleData)
                        symbolProviderForBinariesFromIncrementalCompilation =
                            MetadataSymbolProvider(
                                session,
                                moduleDataProvider,
                                kotlinScopeProvider,
                                precompiledBinariesPackagePartProvider as PackageAndMetadataPartProvider,
                                projectEnvironment.getKotlinClassFinder(it.precompiledBinariesFileScope) as KotlinMetadataFinder,
                                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
                            )
                    }
                }

                listOfNotNull(
                    symbolProvider,
                    *(incrementalCompilationContext?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                    symbolProviderForBinariesFromIncrementalCompilation,
                    generatedSymbolsProvider,
                    *dependencies.toTypedArray(),
                )
            }
        )
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            /**
             * For stdlib and builtin compilation, we don't want to hide @PlatformDependent declarations from the metadata
             */
            FirKotlinScopeProvider { _, declaredScope, _, _, _ -> declaredScope }
        } else {
            FirKotlinScopeProvider()
        }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Nothing?) {}

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Nothing?) {}

    override fun FirSession.registerSourceSessionComponents(c: Nothing?) {
        registerDefaultComponents()
    }

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

}
