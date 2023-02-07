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
import org.jetbrains.kotlin.ir.backend.web.webResolveLibraries
import org.jetbrains.kotlin.ir.backend.web.resolverLogger
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

object TestFirJsSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        module: TestModule,
        testServices: TestServices,
        configuration: CompilerConfiguration,
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession {
        val logger = configuration.resolverLogger
        val libraries = getAllJsDependenciesPaths(module, testServices)
        val resolvedLibraries = webResolveLibraries(libraries, logger).getFullResolvedList()

        return FirJsSessionFactory.createJsLibrarySession(
            mainModuleName,
            resolvedLibraries,
            sessionProvider,
            moduleDataProvider,
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
        FirJsSessionFactory.createJsModuleBasedSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            registerExtraComponents,
            sessionConfigurator
        )
}

fun resolveJsLibraries(
    module: TestModule,
    testServices: TestServices,
    configuration: CompilerConfiguration
): List<KotlinResolvedLibrary> {
    val paths = getAllJsDependenciesPaths(module, testServices)
    val logger = configuration.resolverLogger
    return webResolveLibraries(paths, logger).getFullResolvedList()
}

fun getAllJsDependenciesPaths(module: TestModule, testServices: TestServices): List<String> {
    val (runtimeKlibsPaths, transitiveLibraries, friendLibraries) = getJsDependencies(module, testServices)
    return runtimeKlibsPaths + transitiveLibraries.map { it.path } + friendLibraries.map { it.path }
}

fun getJsDependencies(module: TestModule, testServices: TestServices): Triple<List<String>, List<File>, List<File>> {
    val runtimeKlibsPaths = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
    val transitiveLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = JsEnvironmentConfigurator.getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return Triple(runtimeKlibsPaths, transitiveLibraries, friendLibraries)
}
