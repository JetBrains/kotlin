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
        moduleDataProvider: ModuleDataProvider,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
    ): FirSession {
        val libraries = loadWasmLibraries(module, testServices, configuration)
        val factory = FirWasmSessionFactory.of(configuration.wasmTarget)

        val sharedLibrarySession = factory.createSharedLibrarySession(
            mainModuleName,
            configuration,
            extensionRegistrars
        )

        return factory.createLibrarySession(
            libraries,
            sharedLibrarySession,
            moduleDataProvider,
            extensionRegistrars,
            configuration,
        )
    }

    fun createModuleBasedSession(
        mainModuleData: FirModuleData,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val factory = FirWasmSessionFactory.of(configuration.wasmTarget)
        return factory.createSourceSession(
            mainModuleData,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule = false,
            icData = null,
            init = sessionConfigurator
        )
    }
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
    val runtimeKlibsPaths = WasmEnvironmentConfigurator.getRuntimePathsForModule(target, testServices)
    val transitiveLibraries = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsPaths, transitiveLibraries, friendLibraries)
}
