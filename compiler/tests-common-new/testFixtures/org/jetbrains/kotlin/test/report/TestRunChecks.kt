/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

/**
 * Verifications over a [TestReport] against the set of test ids that were expected to run.
 *
 * These are the checks that do not depend on a backend-specific run model, so both the grouped Wasm runner and
 * Kotlin/Native can share them. The Native-bound ones — `FileCheckMatcher`, exit-code-over-`RunResult` and the
 * package/kind-based `TestFiltering` — deliberately stay in `:native:native.tests`.
 *
 * Each check is pure and returns its verdict instead of throwing, so every runner keeps reporting failures through its
 * own sink: the grouped Wasm runner rethrows per test via `catchingExecutor`, Kotlin/Native reports against the run.
 */
object TestRunChecks {
    sealed interface Result {
        data object Passed : Result

        data class Failed(val reason: String) : Result {
            override fun toString(): String = reason
        }
    }

    /**
     * Ids that were expected to run but carry no outcome at all: their `ProxyLauncher_<hash>` line never appeared
     * (a stripped launcher class, or a VM that died before reaching it), which must fail that specific test rather
     * than let it pass silently.
     */
    fun <ID> findMissingResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val reported = testReport.reportedIds
        return expectedTestIds.filter { it !in reported }
    }

    /** Ids the report carries an outcome for that were not expected — tests that ran but were not part of the batch. */
    fun <ID> findExcessiveResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val expected = expectedTestIds.toSet()
        return testReport.reportedIds.filter { it !in expected }
    }

    /** Fails when the report is empty, i.e. no test produced any outcome. */
    fun <ID> checkNonEmpty(testReport: TestReport<ID>): Result =
        if (testReport.isEmpty()) {
            Result.Failed("No tests have been found. Test report is empty.")
        } else {
            Result.Passed
        }
}
