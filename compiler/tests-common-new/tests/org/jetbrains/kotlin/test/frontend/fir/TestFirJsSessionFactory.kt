/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.resolverLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

object TestFirJsSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        dependencyListForCliModule: DependencyListForCliModule,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        languageVersionSettings: LanguageVersionSettings,
    ): FirSession {
        val repositories = configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList()
        val logger = configuration.resolverLogger
        val libraries = getAllJsDependenciesPaths(module, testServices)
        val resolvedLibraries = jsResolveLibraries(libraries, repositories, logger).getFullResolvedList()

        return FirJsSessionFactory.createJsLibrarySession(
            mainModuleName, resolvedLibraries, sessionProvider, dependencyListForCliModule.moduleDataProvider, languageVersionSettings
        )
    }

    fun createModuleBasedSession(
        mainModuleData: FirModuleDataImpl, sessionProvider: FirProjectSessionProvider, extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings, lookupTracker: LookupTracker?,
        sessionConfigurator: FirSessionConfigurator.() -> Unit
    ): FirSession =
        FirJsSessionFactory.createJsModuleBasedSession(
            mainModuleData, sessionProvider, extensionRegistrars, languageVersionSettings, lookupTracker, sessionConfigurator
        )
}