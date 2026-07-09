/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinWasmStdlibKlibLocation
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Path

class WasmProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<WasmKlibCompilationOperation, WasmKlibCompilationOperation.Builder, WasmHistoryBasedIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    private val registeredModules = mutableSetOf<WasmModule>()

    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        moduleCompilationConfigAction: (WasmKlibCompilationOperation.Builder) -> Unit,
    ): WasmModule {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = WasmModule(
            kotlinToolchain = kotlinToolchain,
            buildSession = kotlinBuild,
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultStrategyConfig = defaultStrategyConfig,
            moduleCompilationConfigAction = moduleCompilationConfigAction,
            stdlibKlibLocation = stdlibClasspath ?: listOf(
                currentKotlinWasmStdlibKlibLocation
            ),
            registeredModules = registeredModules,
        )
        registeredModules.add(module)
        initModule(module, moduleName)
        return module
    }
}

fun BaseCompilationTest.wasmProject(kotlinToolchain: KotlinToolchains, strategyConfig: ExecutionPolicy, action: WasmProject.() -> Unit) {
    kotlinToolchain.assumeWasmIsSupported()
    WasmProject(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}

fun BaseCompilationTest.wasmProject(executionStrategy: CompilerExecutionStrategyConfiguration, action: WasmProject.() -> Unit) {
    wasmProject(executionStrategy.first, executionStrategy.second, action)
}

fun KotlinToolchains.assumeWasmIsSupported() {
    try {
        wasm
    } catch (e: Throwable) {
        if (e.message?.startsWith("Unsupported") == true) {
            assumeTrue(false) { "Kotlin/Wasm is not supported on this version" }
        } else throw e
    }
}
