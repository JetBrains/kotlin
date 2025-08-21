/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator

object TestFirNativeSessionFactory {
    fun createLibrarySession(
        mainModuleName: Name,
        module: TestModule,
        testServices: TestServices,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
    ): FirSession {
        val libraries = loadNativeKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = getAllNativeDependenciesPaths(module, testServices),
            nativeTarget = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
        ).all

        val sharedLibrarySession = FirNativeSessionFactory.createSharedLibrarySession(
            mainModuleName,
            configuration,
            extensionRegistrars,
        )

        return FirNativeSessionFactory.createLibrarySession(
            libraries,
            sharedLibrarySession,
            moduleDataProvider,
            extensionRegistrars,
            configuration,
        )
    }
}

fun getAllNativeDependenciesPaths(module: TestModule, testServices: TestServices) =
    NativeEnvironmentConfigurator.getRuntimePathsForModule(module, testServices) + getTransitivesAndFriendsPaths(module, testServices)
