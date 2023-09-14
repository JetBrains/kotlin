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
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name

object FirNativeSessionFactory : FirAbstractSessionFactory() {
    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinResolvedLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: ((FirSession) -> Unit) = {},
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            registerExtraComponents = { session ->
                session.registerDefaultComponents()
                registerExtraComponents(session)
            },
            createKotlinScopeProvider = { FirKotlinScopeProvider() },
            createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
                val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
                    FORWARD_DECLARATIONS_MODULE_NAME,
                    moduleDataProvider.platform,
                    moduleDataProvider.analyzerServices,
                ).apply {
                    bindSession(session)
                }
                val resolvedKotlinLibraries = resolvedLibraries.map { it.library }
                // KT-61645: stdlib-native must appear before stdlib-common metadata in the dependency list
                // TODO: Consider not reordering libraries after KT-61430 is fixed, and Gradle plugin determines full order of dependencies.
                val (stdlib, otherDeps) = resolvedKotlinLibraries.partition { it.isNativeStdlib }
                val kotlinLibraries = stdlib + otherDeps
                listOfNotNull(
                    KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kotlinLibraries),
                    NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, kotlinLibraries),
                    FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                    syntheticFunctionInterfaceProvider,
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
            null,
            init,
            registerExtraComponents = {
                it.registerDefaultComponents()
                registerExtraComponents(it)
            },
            registerExtraCheckers = { it.registerNativeCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider() },
            createProviders = { _, _, symbolProvider, generatedSymbolsProvider, dependencies ->
                listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    *dependencies.toTypedArray(),
                )
            }
        )
    }
}
