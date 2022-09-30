/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.checkers.registerNativeCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.fir.session.FirAbstractSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.name.Name

object FirNativeSessionFactory : FirAbstractSessionFactory() {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        dependencyListForCliModule: DependencyListForCliModule,
        languageVersionSettings: LanguageVersionSettings
    ): FirSession {
        val moduleDataProvider = dependencyListForCliModule.moduleDataProvider
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            null,
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _ -> declaredMemberScope } },
            createProviders = { session, builtinsModuleData, kotlinScopeProvider ->
                listOf(
                    FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                )
            })
    }

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            null,
            null,
            init,
            registerExtraComponents = { it.registerExtraComponentsForModuleBased() },
            registerExtraCheckers = { it.registerNativeCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _ -> declaredMemberScope } },
            createProviders = { _, _, symbolProvider, generatedSymbolsProvider, dependenciesSymbolProvider ->
                listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    dependenciesSymbolProvider,
                )
            }
        )
    }

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerExtraComponentsForModuleBased() {
        register(FirVisibilityChecker::class, FirVisibilityChecker.Default)
        register(ConeCallConflictResolverFactory::class, NativeCallConflictResolverFactory)
        register(FirPlatformClassMapper::class, FirPlatformClassMapper.Default)
        register(FirOverridesBackwardCompatibilityHelper::class, FirOverridesBackwardCompatibilityHelper.Default())
    }
}