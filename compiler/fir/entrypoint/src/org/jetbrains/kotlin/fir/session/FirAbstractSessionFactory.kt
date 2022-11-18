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
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
abstract class FirAbstractSessionFactory {
    protected fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: ((FirSession) -> Unit),
        createKotlinScopeProvider: () -> FirKotlinScopeProvider,
        createProviders: (FirSession, FirModuleData, FirKotlinScopeProvider) -> List<FirSymbolProvider>
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                sessionProvider.registerSession(it, this)
                it.bindSession(this)
            }

            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerExtraComponents(this)

            val kotlinScopeProvider = createKotlinScopeProvider.invoke()
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val builtinsModuleData = BinaryModuleData.createDependencyModuleData(
                Name.special("<builtins of ${mainModuleName.asString()}"),
                moduleDataProvider.platform,
                moduleDataProvider.analyzerServices,
            )
            builtinsModuleData.bindSession(this)

            val providers = createProviders(this, builtinsModuleData, kotlinScopeProvider)

            val symbolProvider = FirCompositeSymbolProvider(this, providers)
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    protected fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        init: FirSessionConfigurator.() -> Unit,
        registerExtraComponents: ((FirSession) -> Unit),
        registerExtraCheckers: ((FirSessionConfigurator) -> Unit)?,
        createKotlinScopeProvider: () -> FirKotlinScopeProvider,
        createProviders: (
            FirSession, FirKotlinScopeProvider, FirSymbolProvider,
            FirSwitchableExtensionDeclarationsSymbolProvider?,
            FirDependenciesSymbolProvider
        ) -> List<FirSymbolProvider>
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            sessionProvider.registerSession(moduleData, this@session)
            registerModuleData(moduleData)
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker, enumWhenTracker)
            registerExtraComponents(this)

            val kotlinScopeProvider = createKotlinScopeProvider.invoke()
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                registerExtraCheckers?.invoke(this)

                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
                init()
            }.configure()

            val dependenciesSymbolProvider = FirDependenciesSymbolProviderImpl(this)
            val generatedSymbolsProvider = FirSwitchableExtensionDeclarationsSymbolProvider.create(this)

            val providers = createProviders(
                this, kotlinScopeProvider, firProvider.symbolProvider, generatedSymbolsProvider, dependenciesSymbolProvider,
            )

            register(FirSymbolProvider::class, FirCompositeSymbolProvider(this, providers))

            generatedSymbolsProvider?.let { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }
            register(FirDependenciesSymbolProvider::class, dependenciesSymbolProvider)
        }
    }
}