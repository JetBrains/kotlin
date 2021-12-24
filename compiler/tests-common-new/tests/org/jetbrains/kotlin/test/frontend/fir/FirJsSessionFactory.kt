/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

object FirJsSessionFactory {
    @OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
    fun createJsLibrarySession(
        mainModuleName: Name,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                sessionProvider.registerSession(it, this)
                it.bindSession(this)
            }

            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val builtinsModuleData = FirSessionFactory.createModuleDataForBuiltins(
                mainModuleName,
                moduleDataProvider.platform,
                moduleDataProvider.analyzerServices
            ).also { it.bindSession(this@session) }

            val klibProviders = resolveJsLibraries(module, testServices, configuration).map {
                KlibBasedSymbolProvider(this@session, moduleDataProvider, kotlinScopeProvider, it)
            }

            val otherProviders = listOf(
                FirBuiltinSymbolProvider(this, builtinsModuleData, kotlinScopeProvider),
                FirCloneableSymbolProvider(this, builtinsModuleData, kotlinScopeProvider),
                FirDependenciesSymbolProviderImpl(this)
            )

            val symbolProvider = FirCompositeSymbolProvider(this, otherProviders + klibProviders)

            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    @OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
    fun createJsModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker? = null,
        init: FirSessionFactory.FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            sessionProvider.registerSession(moduleData, this@session)
            registerModuleData(moduleData)
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker)

            // Otherwise, the ConeConflictResolverFactory is not found
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider { _, declaredMemberScope, _, _ -> declaredMemberScope }
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

//        val symbolProviderForBinariesFromIncrementalCompilation = providerAndScopeForIncrementalCompilation?.let {
//            KlibBasedSymbolProvider(this@session, SingleModuleDataProvider(moduleData), kotlinScopeProvider, ??)
//        }

            FirSessionFactory.FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
                init()
            }.configure()

            val dependenciesSymbolProvider = FirDependenciesSymbolProviderImpl(this)
            val generatedSymbolsProvider = FirSwitchableExtensionDeclarationsSymbolProvider.create(this)
            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOfNotNull(
                        firProvider.symbolProvider,
                        generatedSymbolsProvider,
                        dependenciesSymbolProvider,
                    )
                )
            )

            generatedSymbolsProvider?.let { register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }

            register(
                FirDependenciesSymbolProvider::class,
                dependenciesSymbolProvider
            )
        }
    }
}