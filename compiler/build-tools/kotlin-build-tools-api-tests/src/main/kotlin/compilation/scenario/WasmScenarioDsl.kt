/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.WasmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.assumeWasmIsSupported

class WasmScenarioDsl(
    override val project: WasmProject,
    override val strategyConfig: ExecutionPolicy,
    override val kotlinToolchains: KotlinToolchains,
) : Scenario<WasmKlibCompilationOperation.Builder, WasmHistoryBasedIncrementalCompilationConfiguration.Builder>

fun BaseCompilationTest.wasmScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<WasmKlibCompilationOperation.Builder, WasmHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    kotlinToolchains.assumeWasmIsSupported()
    action(WasmScenarioDsl(WasmProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig, kotlinToolchains))
}

fun BaseCompilationTest.wasmScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<WasmKlibCompilationOperation.Builder, WasmHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    wasmScenario(executionStrategy.first, executionStrategy.second, action)
}
