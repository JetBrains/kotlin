/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/** The only target-dependent piece of the grouped-batch result protocol: the entry point the executor invokes. */
abstract class GroupedTestsExportedEntryPointGenerator {
    /**
     * Emits the entry point with the target's export annotation (`@JsExport` on wasm-js, `@kotlin.wasm.WasmExport` on
     * wasm-wasi). It must call [runAllFunctionName].
     */
    abstract fun generateExportedEntryPointSource(runAllFunctionName: String): String
}
