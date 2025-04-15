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
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.FirWasmSessionFactory
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getKlibDependencies
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File

object TestFirWasmSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
    ): FirSession {
        val libraries = loadWasmLibraries(module, testServices, configuration)

        val sharedLibrarySession = FirWasmSessionFactory.createSharedLibrarySession(
            mainModuleName,
            sessionProvider,
            configuration,
            extensionRegistrars
        )

        return FirWasmSessionFactory.createLibrarySession(
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
        FirWasmSessionFactory.createSourceSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule = false,
            icData = null,
            init = sessionConfigurator
        )
}

fun loadWasmLibraries(
    module: TestModule,
    testServices: TestServices,
    configuration: CompilerConfiguration,
): List<KotlinLibrary> {
    return loadWebKlibsInTestPipeline(
        configuration = configuration,
        libraryPaths = getAllWasmDependenciesPaths(module, testServices, configuration.wasmTarget),
        platformChecker = KlibPlatformChecker.Wasm(configuration.wasmTarget.alias),
    ).all
}

fun getAllWasmDependenciesPaths(
    module: TestModule,
    testServices: TestServices,
    target: WasmTarget,
): List<String> {
    val (runtimeKlibsPaths, transitiveLibraries, friendLibraries) = getWasmDependencies(module, testServices, target)
    return runtimeKlibsPaths + transitiveLibraries.map { it.path } + friendLibraries.map { it.path }
}

fun getWasmDependencies(
    module: TestModule,
    testServices: TestServices,
    target: WasmTarget,
): Triple<List<String>, List<File>, List<File>> {
    val runtimeKlibsPaths = WasmEnvironmentConfigurator.getRuntimePathsForModule(target)
    val transitiveLibraries = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsPaths, transitiveLibraries, friendLibraries)
}
