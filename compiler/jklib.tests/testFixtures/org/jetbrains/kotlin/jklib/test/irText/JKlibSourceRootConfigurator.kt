/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jklib.config.jklibOutputDestination
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.klibPaths
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.addSourcesForDependsOnClosure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import java.io.File

class JKlibSourceRootConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addSourcesForDependsOnClosure(module, testServices)

        val stdlibKlib = System.getProperty("kotlin.stdlib.jklib.for.test")
            ?: error("kotlin.stdlib.jvm.ir.klib system property is not set")
        configuration.klibPaths += stdlibKlib

        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("klib-output")
        val outputFile = File(tempDir, "${module.name}.klib")
        configuration.jklibOutputDestination = outputFile.absolutePath

        // In JKlib's JVM-based test infrastructure, compiled dependency modules are resolved
        // and registered as directories of JVM class files under the JVM classpath instead of KLibs.
        // Because JKlib is a KLib-based backend and only resolves symbols from actual KLib binaries,
        // we map each module dependency to its expected, deterministic JKLIB output path.
        // Since these dependencies are compiled sequentially, these KLib binaries will exist on disk
        // by the time this module's compilation phase actually executes.
        val klibs = module.allDependencies.map { File(tempDir, "${it.dependencyModule.name}.klib") }
        if (klibs.isNotEmpty()) {
            configuration.klibPaths += klibs.map { it.absolutePath }
        }
    }
}
