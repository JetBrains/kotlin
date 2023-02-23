/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.checkers.registerNativeCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultExtraComponentsForModuleBased
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name

object FirNativeSessionFactory : FirAbstractSessionFactory() {
    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinResolvedLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: ((FirSession) -> Unit) = {},
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            registerExtraComponents,
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _ -> declaredMemberScope } },
            createProviders = { session, builtinsModuleData, kotlinScopeProvider ->
                val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
                    Name.special("<forward declarations>"),
                    moduleDataProvider.platform,
                    moduleDataProvider.analyzerServices,
                ).apply {
                    bindSession(session)
                }
                listOf(
                    KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries),
                    NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, resolvedLibraries),
                    FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirExtensionSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                )
            })
    }

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        init: FirSessionConfigurator.() -> Unit,
        registerExtraComponents: ((FirSession) -> Unit) = {},
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            null,
            null,
            init,
            registerExtraComponents = { session ->
                session.registerDefaultExtraComponentsForModuleBased()
                registerExtraComponents(session)
            },
            registerExtraCheckers = { it.registerNativeCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider { _, declaredMemberScope, _, _ -> declaredMemberScope } },
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
}
