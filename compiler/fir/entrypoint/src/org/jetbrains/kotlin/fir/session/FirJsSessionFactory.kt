/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirVisibilityChecker
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.FirEmptyOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.checkers.registerJsCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name

object FirJsSessionFactory : FirAbstractSessionFactory() {
    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker?,
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
            init,
            registerExtraComponents = { session ->
                session.registerJsSpecificComponents()
                registerExtraComponents(session)
            },
            registerExtraCheckers = { it.registerJsCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope } },
            createProviders = { _, _, symbolProvider, generatedSymbolsProvider, syntheticFunctionInterfaceProvider, dependencies ->
                listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    syntheticFunctionInterfaceProvider,
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
            it.registerJsSpecificComponents()
            registerExtraComponents(it)
        },
        createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _, _ -> declaredMemberScope } },
        createProviders = { session, builtinsModuleData, kotlinScopeProvider ->
            listOfNotNull(
                KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries),
                // (Most) builtins should be taken from the dependencies in JS compilation, therefore builtins provider is the last one
                // TODO: consider using "poisoning" provider for builtins to ensure that proper ones are taken from dependencies
                // NOTE: it requires precise filtering for true builtins, like Function*
                FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(session, builtinsModuleData, kotlinScopeProvider),
            )
        }
    )

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerJsSpecificComponents() {
        register(FirVisibilityChecker::class, FirVisibilityChecker.Default)
        register(ConeCallConflictResolverFactory::class, JsCallConflictResolverFactory)
        register(FirPlatformClassMapper::class, FirPlatformClassMapper.Default)
        register(FirOverridesBackwardCompatibilityHelper::class, FirEmptyOverridesBackwardCompatibilityHelper)
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())
    }
}
