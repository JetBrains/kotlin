/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/**
 * The target-dependent piece of the grouped-batch result protocol:
 * emits the executor-invoked exported entry point that drives the generated result-collecting runner.
 */
abstract class GroupedTestsExportedEntryPointGenerator {
    /**
     * Emits the target-specific exported entry point, carrying that target's export annotation — `@JsExport` on
     * wasm-js, `@kotlin.wasm.WasmExport` on wasm-wasi — so that the executor can invoke it.
     *
     * The emitted function must call [runAllFunctionName], the generated function that runs and reports every test.
     * Its source is appended to the generated launcher by
     * [GroupedTestsResultProtocol.generateResultCollectingRunnerSource], which supplies [runAllFunctionName].
     */
    abstract fun generateExportedEntryPointSource(runAllFunctionName: String): String
}
