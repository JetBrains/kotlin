/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.useFirExtraCheckers
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.PlatformConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeCastChecker
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.backend.native.FirNativeClassMapper
import org.jetbrains.kotlin.fir.backend.native.FirNativeOverrideChecker
import org.jetbrains.kotlin.fir.checkers.registerNativeCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraNativeCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices

@OptIn(SessionConfiguration::class)
object FirNativeSessionFactory : FirAbstractSessionFactory<Nothing?, Nothing?>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinResolvedLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
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
                val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
                    FORWARD_DECLARATIONS_MODULE_NAME,
                    moduleDataProvider.platform,
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

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSession.registerLibrarySessionComponents(c: Nothing?) {
        registerComponents()
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            context = null,
            sessionProvider,
            extensionRegistrars,
            configuration,
            init,
            createProviders = { _, _, symbolProvider, generatedSymbolsProvider, dependencies ->
                listOfNotNull(
                    symbolProvider,
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
        return FirKotlinScopeProvider()
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Nothing?) {
        registerNativeCheckers()
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Nothing?) {
        registerExtraNativeCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: Nothing?) {
        registerComponents()
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents() {
        registerDefaultComponents()
        registerNativeComponents()
    }

    // ==================================== Utilities ====================================

    fun FirSession.registerNativeComponents() {
        register(FirPlatformClassMapper::class, FirNativeClassMapper())
        register(FirPlatformSpecificCastChecker::class, FirNativeCastChecker)
        register(PlatformConflictDeclarationsDiagnosticDispatcher::class, NativeConflictDeclarationsDiagnosticDispatcher)
        register(FirOverrideChecker::class, FirNativeOverrideChecker(this))
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(NativePlatformAnalyzerServices))
    }
}
