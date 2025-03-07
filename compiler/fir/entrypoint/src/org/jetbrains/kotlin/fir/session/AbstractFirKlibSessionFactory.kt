/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name

@OptIn(SessionConfiguration::class)
abstract class AbstractFirKlibSessionFactory<LIBRARY_CONTEXT, SOURCE_CONTEXT> : FirAbstractSessionFactory<LIBRARY_CONTEXT, SOURCE_CONTEXT>() {

    // ==================================== Shared library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSharedLibrarySession]
     */
    fun createSharedLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
    ): FirSession {
        return createSharedLibrarySession(
            mainModuleName,
            createLibraryContext(configuration),
            sessionProvider,
            configuration.languageVersionSettings,
            extensionRegistrars,
        ) { session, moduleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
            listOfNotNull(
                FirBuiltinSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider),
                syntheticFunctionInterfaceProvider
            )
        }
    }

    // ==================================== Library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createLibrarySession]
     */
    fun createLibrarySession(
        resolvedLibraries: List<KotlinLibrary>,
        sessionProvider: FirProjectSessionProvider,
        sharedLibrarySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        compilerConfiguration: CompilerConfiguration,
    ): FirSession {
        val context = createLibraryContext(compilerConfiguration)
        return createLibrarySession(
            context,
            sharedLibrarySession,
            sessionProvider,
            moduleDataProvider,
            compilerConfiguration.languageVersionSettings,
            extensionRegistrars,
            createProviders = { session, kotlinScopeProvider ->
                listOfNotNull(
                    KlibBasedSymbolProvider(
                        session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries,
                        flexibleTypeFactory = createFlexibleTypeFactory(session),
                    ),
                    *createAdditionalDependencyProviders(session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries).toTypedArray(),
                )
            }
        )
    }

    protected abstract fun createLibraryContext(configuration: CompilerConfiguration): LIBRARY_CONTEXT

    protected open fun createFlexibleTypeFactory(session: FirSession): FirTypeDeserializer.FlexibleTypeFactory {
        return FirTypeDeserializer.FlexibleTypeFactory.Default
    }

    protected open fun createAdditionalDependencyProviders(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        resolvedLibraries: List<KotlinLibrary>,
    ): List<FirSymbolProvider> {
        return emptyList()
    }

    final override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    // ==================================== Platform session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSourceSession]
     */
    fun createSourceSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        icData: KlibIcData? = null,
        init: FirSessionConfigurator.() -> Unit
    ): FirSession {
        val context = createSourceContext(configuration)
        return createSourceSession(
            moduleData,
            context,
            sessionProvider,
            extensionRegistrars,
            configuration,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider ->
                SourceProviders(
                    listOfNotNull(
                        symbolProvider,
                        generatedSymbolsProvider,
                        icData?.let {
                            KlibIcCacheBasedSymbolProvider(
                                session,
                                SingleModuleDataProvider(moduleData),
                                kotlinScopeProvider,
                                it,
                                flexibleTypeFactory = createFlexibleTypeFactory(session),
                            )
                        },
                    )
                )
            }
        )
    }

    protected abstract fun createSourceContext(configuration: CompilerConfiguration): SOURCE_CONTEXT

    final override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }
}
