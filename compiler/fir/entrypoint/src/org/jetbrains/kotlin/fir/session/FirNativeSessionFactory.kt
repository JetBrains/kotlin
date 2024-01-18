/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.PlatformConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeCastChecker
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.backend.native.FirNativeClassMapper
import org.jetbrains.kotlin.fir.backend.native.FirNativeOverrideChecker
import org.jetbrains.kotlin.fir.checkers.registerNativeCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
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
                session.registerNativeComponents()
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
                listOfNotNull(
                    KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedKotlinLibraries),
                    NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, resolvedKotlinLibraries),
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
                it.registerNativeComponents()
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

    @OptIn(SessionConfiguration::class)
    fun FirSession.registerNativeComponents() {
        register(FirPlatformClassMapper::class, FirNativeClassMapper())
        register(FirPlatformSpecificCastChecker::class, FirNativeCastChecker)
        register(PlatformConflictDeclarationsDiagnosticDispatcher::class, NativeConflictDeclarationsDiagnosticDispatcher)
        register(FirOverrideChecker::class, FirNativeOverrideChecker(this))
    }
}
