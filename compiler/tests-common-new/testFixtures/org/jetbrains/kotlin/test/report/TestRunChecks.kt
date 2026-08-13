/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

/**
 * Verifications over a [TestReport] against the test ids that were expected to run. The backend-independent ones only:
 * the Native-bound checks stay in `:native:native.tests`.
 *
 * Each check returns its verdict instead of throwing, so every runner keeps reporting failures through its own sink.
 */
object TestRunChecks {
    /** Ids expected to run that carry no outcome at all — a test that must be failed instead of passing silently. */
    fun <ID> findMissingResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val reported = testReport.reportedIds
        return expectedTestIds.filter { it !in reported }
    }

    fun <ID> findExcessiveResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val expected = expectedTestIds.toSet()
        return testReport.reportedIds.filter { it !in expected }
    }

    /** The reason the report cannot be trusted, or `null` when it carries at least one outcome. */
    fun <ID> emptyReportReason(testReport: TestReport<ID>): String? =
        "No tests have been found. Test report is empty.".takeIf { testReport.isEmpty() }
}
