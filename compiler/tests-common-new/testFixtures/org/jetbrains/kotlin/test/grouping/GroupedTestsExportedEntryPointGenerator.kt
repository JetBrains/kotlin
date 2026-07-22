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
     * Emits the target-specific exported entry point that having target's export annotation, to be invoked by the executor.
     * It must call [runAllFunctionName] (the generated function that runs and reports every test).
     * Please append the output of `generateExportedEntryPointSource()` (as the driver's entry point) to the generated launcher source.
     */
    abstract fun generateExportedEntryPointSource(runAllFunctionName: String): String
}
