/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

/**
 * Checks WASM diagnostics in the test data with reversed resolution order.
 *
 * A counterpart for [AbstractLLWasmDiagnosticsTest].
 *
 * @see AbstractLLWasmDiagnosticsTest
 */
abstract class AbstractLLReversedWasmDiagnosticsTest(
    private val targetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
) : AbstractLLReversedDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureForLLWasmDiagnosticsTest(targetPlatform, wasmTarget)
    }
}

abstract class AbstractLLReversedWasmJsDiagnosticsTest : AbstractLLWasmDiagnosticsTest(
    WasmPlatforms.wasmJs,
    WasmTarget.JS,
)

abstract class AbstractLLReversedWasmWasiDiagnosticsTest : AbstractLLWasmDiagnosticsTest(
    WasmPlatforms.wasmWasi,
    WasmTarget.WASI,
)


