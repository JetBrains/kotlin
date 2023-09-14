/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.checkers.registerWasmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name

object FirWasmSessionFactory : FirAbstractSessionFactory() {
    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker?,
        icData: KlibIcData? = null,
        registerExtraComponents: ((FirSession) -> Unit) = {},
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            null,
            null,
            init,
            registerExtraComponents = {
                it.registerDefaultComponents()
                registerExtraComponents(it)
            },
            registerExtraCheckers = { it.registerWasmCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope } },
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

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession = createLibrarySession(
        mainModuleName,
        sessionProvider,
        moduleDataProvider,
        languageVersionSettings,
        extensionRegistrars,
        registerExtraComponents = {
            it.registerDefaultComponents()
            registerExtraComponents(it)
        },
        createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope } },
        createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
            listOfNotNull(
                KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries),
                FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                syntheticFunctionInterfaceProvider,
            )
        }
    )
}
