/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

object TestFirNativeSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        module: TestModule,
        testServices: TestServices,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        registerExtraComponents: (FirSession) -> Unit = {},
    ): FirSession {
        val resolvedLibraries = CommonKLibResolver.resolve(
            getAllNativeDependenciesPaths(module, testServices),
            configuration.getLogger(treatWarningsAsErrors = true),
            knownIrProviders = listOf("kotlin.native.cinterop"), // FIXME use KonanLibraryProperResolver instead, as in production.
        ).getFullResolvedList()

        return FirNativeSessionFactory.createLibrarySession(
            mainModuleName,
            resolvedLibraries,
            sessionProvider,
            moduleDataProvider,
            extensionRegistrars,
            languageVersionSettings,
            registerExtraComponents,
        )
    }
}

fun getAllNativeDependenciesPaths(module: TestModule, testServices: TestServices) =
    NativeEnvironmentConfigurator.getRuntimePathsForModule(module, testServices) + getTransitivesAndFriendsPaths(module, testServices)
