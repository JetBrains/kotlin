/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsModuleKind
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.checkers.registerJsCheckers
import org.jetbrains.kotlin.fir.declarations.FirTypeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.js.ModuleKind

object FirJsSessionFactory : FirAbstractSessionFactory() {
    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        compilerConfiguration: CompilerConfiguration,
        lookupTracker: LookupTracker?,
        icData: KlibIcData? = null,
        registerExtraComponents: ((FirSession) -> Unit) = {},
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            compilerConfiguration.languageVersionSettings,
            lookupTracker,
            null,
            null,
            init,
            registerExtraComponents = {
                it.registerDefaultComponents()
                it.registerJsComponents(compilerConfiguration)
                registerExtraComponents(it)
            },
            registerExtraCheckers = { it.registerJsCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider() },
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
        compilerConfiguration: CompilerConfiguration,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession = createLibrarySession(
        mainModuleName,
        sessionProvider,
        moduleDataProvider,
        compilerConfiguration.languageVersionSettings,
        extensionRegistrars,
        registerExtraComponents = {
            it.registerDefaultComponents()
            it.registerJsComponents(compilerConfiguration)
            registerExtraComponents(it)
        },
        createKotlinScopeProvider = { FirKotlinScopeProvider() },
        createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
            listOfNotNull(
                KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries),
                FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                syntheticFunctionInterfaceProvider
            )
        }
    )

    @OptIn(SessionConfiguration::class)
    private fun FirSession.registerJsComponents(compilerConfiguration: CompilerConfiguration) {
        register(ConeCallConflictResolverFactory::class, JsCallConflictResolverFactory)
        register(
            FirTypeSpecificityComparatorProvider::class,
            FirTypeSpecificityComparatorProvider(JsTypeSpecificityComparatorWithoutDelegate(typeContext))
        )
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())

        val moduleKind = compilerConfiguration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        register(FirJsModuleKind::class, FirJsModuleKind(moduleKind))
    }
}
