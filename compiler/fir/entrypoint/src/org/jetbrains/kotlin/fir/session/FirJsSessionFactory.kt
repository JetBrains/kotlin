/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.useFirExtraCheckers
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.FirIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsModuleKind
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.checkers.registerJsCheckers
import org.jetbrains.kotlin.fir.declarations.FirTypeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.js.ModuleKind

@OptIn(SessionConfiguration::class)
object FirJsSessionFactory : FirAbstractSessionFactory<FirJsSessionFactory.Context, FirJsSessionFactory.Context>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        compilerConfiguration: CompilerConfiguration,
    ): FirSession {
        val context = Context(compilerConfiguration)
        return createLibrarySession(
            mainModuleName,
            context,
            sessionProvider,
            moduleDataProvider,
            compilerConfiguration.languageVersionSettings,
            extensionRegistrars,
            createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
                listOfNotNull(
                    KlibBasedSymbolProvider(
                        session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries,
                        flexibleTypeFactory = JsFlexibleTypeFactory(session),
                    ),
                    FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
                    syntheticFunctionInterfaceProvider
                )
            }
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerComponents(c.configuration)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        compilerConfiguration: CompilerConfiguration,
        lookupTracker: LookupTracker?,
        icData: KlibIcData? = null,
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        val context = Context(compilerConfiguration)
        return createModuleBasedSession(
            moduleData,
            context,
            sessionProvider,
            extensionRegistrars,
            compilerConfiguration.languageVersionSettings,
            compilerConfiguration.useFirExtraCheckers,
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
                            flexibleTypeFactory = JsFlexibleTypeFactory(session),
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
        return FirKotlinScopeProvider()
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Context) {
        registerJsCheckers()
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Context) {}

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerComponents(c.configuration)
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents(compilerConfiguration: CompilerConfiguration) {
        val moduleKind = compilerConfiguration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        registerDefaultComponents()
        registerJsComponents(moduleKind)
    }

    fun FirSession.registerJsComponents(moduleKind: ModuleKind?) {
        register(ConeCallConflictResolverFactory::class, JsCallConflictResolverFactory)
        register(
            FirTypeSpecificityComparatorProvider::class,
            FirTypeSpecificityComparatorProvider(JsTypeSpecificityComparatorWithoutDelegate(typeContext))
        )
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())
        register(FirIdentityLessPlatformDeterminer::class, FirJsIdentityLessPlatformDeterminer)

        if (moduleKind != null) {
            register(FirJsModuleKind::class, FirJsModuleKind(moduleKind))
        }
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(JsPlatformAnalyzerServices))
    }

    // ==================================== Utilities ====================================

    class Context(val configuration: CompilerConfiguration)
}
