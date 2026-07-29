/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

/**
 * Target-independent verifications over a [TestReport], given the set of test ids that were *expected* to run.
 *
 * These are the checks that do not depend on any backend-specific run model, extracted so both the grouped K/Wasm
 * runner and (in a later convergence) Kotlin/Native can share them. The Native-bound checks — `FileCheckMatcher`,
 * exit-code-over-`RunResult`, and package/kind-based `TestFiltering` — deliberately stay in `:native:native.tests`.
 *
 * Each verifier is pure and generic over the id type [ID]; it returns a [Result] (or a list of missing/
 * excessive ids) rather than throwing, so callers keep control of how a failure is surfaced.
 *
 * TODO KT-87785: Move here all shared functionality from [org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks]
 */
object TestRunChecks {
    /**
     * Outcome of a single target-independent check over a [TestReport] (see [TestRunChecks]).
     *
     * Mirrors the shape of the Kotlin/Native `TestRunCheck.Result`
     * (`org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.Result`), so that a future convergence can unify the two.
     * A check returns a [Failed] with a human-readable [reason] instead of throwing, letting each runner translate it into its own failure sink:
     * - Wasm rethrows it through the per-test `catchingExecutor`;
     * - K/N reports it against the test run.
     */
    sealed interface Result {
        data object Passed : Result

        data class Failed(val reason: String) : Result {
            override fun toString(): String = reason
        }
    }

    /**
     * Ids that were expected to run but for which the report carries no outcome at all. On the executor/VM side a missing
     * id means the test's `ProxyLauncher_<hash>` line never appeared (e.g. the launcher class was stripped, or a VM
     * crashed before reaching it) — which must fail that specific test rather than let it pass silently.
     */
    fun <ID> findMissingResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val reported = testReport.reportedIds
        return expectedTestIds.filter { it !in reported }
    }

    /**
     * Ids the report carries an outcome for that were not in [expectedTestIds] — i.e. tests that ran but were not
     * part of the batch. Analogous to Kotlin/Native's "excessive tests have been executed" check.
     */
    fun <ID> findExcessiveResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val expected = expectedTestIds.toSet()
        return testReport.reportedIds.filter { it !in expected }
    }

    /** Fails when the report is empty (no test produced any outcome). */
    fun <ID> checkNonEmpty(testReport: TestReport<ID>): Result =
        if (testReport.isEmpty()) {
            Result.Failed("No tests have been found. Test report is empty.")
        } else {
            Result.Passed
        }
}
