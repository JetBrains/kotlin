/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

object TestFirJsSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession {
        val resolvedLibraries = resolveLibraries(configuration, getAllJsDependenciesPaths(module, testServices))

        return FirJsSessionFactory.createLibrarySession(
            mainModuleName,
            resolvedLibraries.map { it.library },
            sessionProvider,
            moduleDataProvider,
            extensionRegistrars,
            configuration,
            registerExtraComponents,
        )
    }

    fun createModuleBasedSession(
        mainModuleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        lookupTracker: LookupTracker?,
        registerExtraComponents: ((FirSession) -> Unit),
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
    ): FirSession =
        FirJsSessionFactory.createModuleBasedSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            configuration,
            lookupTracker,
            icData = null,
            registerExtraComponents,
            sessionConfigurator
        )
}

fun getAllJsDependenciesPaths(module: TestModule, testServices: TestServices): List<String> {
    return JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices) + getTransitivesAndFriendsPaths(module, testServices)
}
