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
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.FirWasmSessionFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.resolverLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
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
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession {
        val target = configuration.get(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)
        val resolvedLibraries = resolveLibraries(
            configuration = configuration,
            paths = getAllWasmDependenciesPaths(module, testServices, target)
        )

        return FirWasmSessionFactory.createLibrarySession(
            mainModuleName,
            resolvedLibraries.map { it.library },
            sessionProvider,
            moduleDataProvider,
            extensionRegistrars,
            languageVersionSettings,
            registerExtraComponents,
        )
    }

    fun createModuleBasedSession(
        mainModuleData: FirModuleData, sessionProvider: FirProjectSessionProvider, extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings, lookupTracker: LookupTracker?,
        registerExtraComponents: ((FirSession) -> Unit),
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
    ): FirSession =
        FirWasmSessionFactory.createModuleBasedSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            icData = null,
            registerExtraComponents,
            sessionConfigurator
        )
}

fun resolveWasmLibraries(
    module: TestModule,
    testServices: TestServices,
    configuration: CompilerConfiguration
): List<KotlinResolvedLibrary> {
    val paths = getAllWasmDependenciesPaths(
        module = module,
        testServices = testServices,
        target = configuration.get(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)
    )
    return resolveLibraries(configuration, paths)
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
    val transitiveLibraries = WasmEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = WasmEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsPaths, transitiveLibraries, friendLibraries)
}