/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerWasmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.resolve.WasmPlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmWasiPlatformAnalyzerServices

@OptIn(SessionConfiguration::class)
object FirWasmSessionFactory : FirAbstractSessionFactory<FirWasmSessionFactory.Context, FirWasmSessionFactory.Context>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        wasmTarget: WasmTarget,
    ): FirSession = createLibrarySession(
        mainModuleName,
        Context(wasmTarget),
        sessionProvider,
        moduleDataProvider,
        languageVersionSettings,
        extensionRegistrars,
        createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
            listOfNotNull(
                KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries),
                FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                syntheticFunctionInterfaceProvider,
            )
        }
    )

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope }
    }

    override fun FirSession.registerLibrarySessionComponents(c: FirWasmSessionFactory.Context) {
        registerDefaultComponents()
        registerWasmComponents(c.wasmTarget)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        wasmTarget: WasmTarget,
        lookupTracker: LookupTracker?,
        icData: KlibIcData? = null,
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            Context(wasmTarget),
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker = null,
            importTracker = null,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependencies ->
                listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    icData?.let {
                        KlibIcCacheBasedSymbolProvider(
                            session,
                            SingleModuleDataProvider(moduleData),
                            kotlinScopeProvider,
                            it,
                        )
                    },
                    *dependencies.toTypedArray(),
                )
            }
        )
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Context) {
        registerWasmCheckers(c.wasmTarget)
    }

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerDefaultComponents()
        registerWasmComponents(c.wasmTarget)
    }

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerWasmComponents(wasmTarget: WasmTarget) {
        val analyzerServices = when (wasmTarget) {
            WasmTarget.JS -> WasmPlatformAnalyzerServices
            WasmTarget.WASI -> WasmWasiPlatformAnalyzerServices
        }
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(analyzerServices))
    }

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    class Context(val wasmTarget: WasmTarget)
}
