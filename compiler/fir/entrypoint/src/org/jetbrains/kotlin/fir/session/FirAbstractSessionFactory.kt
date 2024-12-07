/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
abstract class FirAbstractSessionFactory<LIBRARY_CONTEXT, SOURCE_CONTEXT> {

    // ==================================== Library session ====================================

    protected fun createLibrarySession(
        mainModuleName: Name,
        context: LIBRARY_CONTEXT,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings,
        extensionRegistrars: List<FirExtensionRegistrar>,
        createProviders: (FirSession, FirModuleData, FirKotlinScopeProvider, FirExtensionSyntheticFunctionInterfaceProvider?) -> List<FirSymbolProvider>
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                sessionProvider.registerSession(it, this)
                it.bindSession(this)
            }

            registerCliCompilerOnlyComponents(languageVersionSettings)
            registerCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            val kotlinScopeProvider = createKotlinScopeProviderForLibrarySession()
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val builtinsModuleData = BinaryModuleData.createDependencyModuleData(
                Name.special("<builtins of ${mainModuleName.asString()}"),
                moduleDataProvider.platform,
            )
            builtinsModuleData.bindSession(this)

            FirSessionConfigurator(this).apply {
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val syntheticFunctionInterfaceProvider =
                FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(this, builtinsModuleData, kotlinScopeProvider)
            val providers = createProviders(this, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider)

            val symbolProvider = FirCachingCompositeSymbolProvider(this, providers)
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    protected abstract fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider
    protected abstract fun FirSession.registerLibrarySessionComponents(c: LIBRARY_CONTEXT)

    // ==================================== Platform session ====================================

    protected fun createModuleBasedSession(
        moduleData: FirModuleData,
        context: SOURCE_CONTEXT,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        init: FirSessionConfigurator.() -> Unit,
        createProviders: (
            FirSession, FirKotlinScopeProvider, FirSymbolProvider,
            FirSwitchableExtensionDeclarationsSymbolProvider?,
            dependencies: List<FirSymbolProvider>,
        ) -> List<FirSymbolProvider>
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            sessionProvider.registerSession(moduleData, this@session)
            registerModuleData(moduleData)
            registerCliCompilerOnlyComponents(languageVersionSettings)
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker, enumWhenTracker, importTracker)
            registerSourceSessionComponents(context)

            val kotlinScopeProvider = createKotlinScopeProviderForSourceSession(moduleData, languageVersionSettings)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                registerPlatformCheckers(context)

                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
                init()
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProviders = computeDependencyProviderList(moduleData)
            val generatedSymbolsProvider = FirSwitchableExtensionDeclarationsSymbolProvider.createIfNeeded(this)

            val providers = createProviders(
                this,
                kotlinScopeProvider,
                firProvider.symbolProvider,
                generatedSymbolsProvider,
                dependencyProviders,
            )

            register(
                FirSymbolProvider::class,
                FirCachingCompositeSymbolProvider(
                    this, providers,
                    expectedCachesToBeCleanedOnce = generatedSymbolsProvider != null
                )
            )

            generatedSymbolsProvider?.let { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, FirCachingCompositeSymbolProvider(this, dependencyProviders))
        }
    }

    protected abstract fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData, languageVersionSettings: LanguageVersionSettings
    ): FirKotlinScopeProvider

    protected abstract fun FirSessionConfigurator.registerPlatformCheckers(c: SOURCE_CONTEXT)
    protected abstract fun FirSession.registerSourceSessionComponents(c: SOURCE_CONTEXT)

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    private fun FirSession.computeDependencyProviderList(moduleData: FirModuleData): List<FirSymbolProvider> {
        // dependsOnDependencies can actualize declarations from their dependencies. Because actual declarations can be more specific
        // (e.g. have additional supertypes), the modules must be ordered from most specific (i.e. actual) to most generic (i.e. expect)
        // to prevent false positive resolution errors (see KT-57369 for an example).
        return (moduleData.dependencies + moduleData.friendDependencies + moduleData.allDependsOnDependencies)
            .mapNotNull { sessionProvider?.getSession(it) }
            .flatMap { it.symbolProvider.flatten() }
            .distinct()
            .sortedBy { it.session.kind }
    }

    /* It eliminates dependency and composite providers since the current dependency provider is composite in fact.
    *  To prevent duplications and resolving errors, library or source providers from other modules should be filtered out during flattening.
    *  It depends on the session's kind of the top-level provider */
    private fun FirSymbolProvider.flatten(): List<FirSymbolProvider> {
        val originalSession = session.takeIf { it.kind == FirSession.Kind.Source }
        val result = mutableListOf<FirSymbolProvider>()

        fun FirSymbolProvider.collectProviders() {
            when {
                // When provider is composite, unwrap all contained providers and recurse.
                this is FirCachingCompositeSymbolProvider -> {
                    for (provider in providers) {
                        provider.collectProviders()
                    }
                }

                // Make sure only source symbol providers from the same session as the original symbol provider are flattened. A composite
                // symbol provider can contain source symbol providers from multiple sessions that may represent dependency symbol providers
                // which should not be propagated transitively.
                originalSession != null && session.kind == FirSession.Kind.Source && session == originalSession ||
                        originalSession == null && session.kind == FirSession.Kind.Library -> {
                    result.add(this)
                }
            }
        }

        collectProviders()

        return result
    }
}
