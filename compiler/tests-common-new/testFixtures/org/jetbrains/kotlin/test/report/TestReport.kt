/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

/**
 * Per-test outcomes of a single (possibly batched) test run, keyed by an opaque test id of type [ID].
 *
 * Extracted from the Kotlin/Native `TestReport` (keyed by `TestName`) so that the grouped Wasm runners,
 * which key by their own synthetic launcher class name, can reuse the same holder and the checks over it.
 */
data class TestReport<ID>(
    val passedTests: Set<ID>,
    val failedTests: Set<ID>,
    val ignoredTests: Set<ID>,
) {
    fun isEmpty(): Boolean = passedTests.isEmpty() && failedTests.isEmpty() && ignoredTests.isEmpty()

    /** All ids the run reported an outcome for, regardless of status. */
    val reportedIds: Set<ID> get() = passedTests + failedTests + ignoredTests

    override fun toString(): String = """
        TestReport:
         * Passed:  $passedTests
         * Failed:  $failedTests
         * Ignored: $ignoredTests
    """.trimIndent()
}
