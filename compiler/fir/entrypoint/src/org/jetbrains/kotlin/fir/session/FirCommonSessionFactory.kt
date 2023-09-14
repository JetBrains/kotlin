/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
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

object FirCommonSessionFactory : FirAbstractSessionFactory() {
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
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            registerExtraComponents = {
                it.registerDefaultComponents()
                registerExtraComponents(it)
            },
            createKotlinScopeProvider = { FirKotlinScopeProvider() },
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
                    FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                )
            }
        )
    }

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker? = null,
        enumWhenTracker: EnumWhenTracker? = null,
        importTracker: ImportTracker? = null,
        registerExtraComponents: ((FirSession) -> Unit) = {},
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            init,
            registerExtraComponents = {
                it.registerDefaultComponents()
                registerExtraComponents(it)
            },
            registerExtraCheckers = {},
            createKotlinScopeProvider = { FirKotlinScopeProvider() },
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
}
