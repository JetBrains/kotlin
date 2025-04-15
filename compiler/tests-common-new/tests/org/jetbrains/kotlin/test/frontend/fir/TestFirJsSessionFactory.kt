/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
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
    ): FirSession {
        val libraries = loadWebKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = getAllJsDependenciesPaths(module, testServices),
            platformChecker = KlibPlatformChecker.JS,
        ).all

        val sharedLibrarySession = FirJsSessionFactory.createSharedLibrarySession(
            mainModuleName,
            sessionProvider,
            configuration,
            extensionRegistrars,
        )

        return FirJsSessionFactory.createLibrarySession(
            libraries,
            sessionProvider,
            sharedLibrarySession,
            moduleDataProvider,
            extensionRegistrars,
            configuration,
        )
    }

    fun createModuleBasedSession(
        mainModuleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
    ): FirSession =
        FirJsSessionFactory.createSourceSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule = false,
            icData = null,
            sessionConfigurator
        )
}

fun getAllJsDependenciesPaths(module: TestModule, testServices: TestServices): List<String> {
    return JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices) + getTransitivesAndFriendsPaths(module, testServices)
}
