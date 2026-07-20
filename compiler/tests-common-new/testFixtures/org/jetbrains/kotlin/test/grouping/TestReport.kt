/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/**
 * Target-independent per-test outcome report of a single (possibly grouped/batched) test run: the set of tests
 * that [passedTests], [failedTests], and [ignoredTests] were reported for, keyed by an opaque test id of type [ID].
 *
 * This is the shared shape of the long-standing Kotlin/Native `TestReport`
 * (`org.jetbrains.kotlin.konan.test.blackbox.support.util.TestReport`, keyed by `TestName`), extracted here so that
 * the grouped K/Wasm runner can reuse it. Wasm keys by the stable `ProxyLauncher_<hash>` id (`ID = String`); a
 * future convergence can have K/N use `ID = TestName` (see [TestRunChecks]).
 *
 * The holder is deliberately free of any transport/protocol detail (no raw outcomes, messages, or stack traces):
 * those live next to the producer (e.g. `GroupedTestsResultProtocol.ParsedBatchResult.outcomes`), so verification
 * over the report is decoupled from how the results were transmitted from the VM.
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
