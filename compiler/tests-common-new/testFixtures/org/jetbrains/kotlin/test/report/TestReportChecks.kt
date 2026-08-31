/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

object TestReportChecks {
    sealed interface Result {
        data object Passed : Result

        data class Failed(val reason: String) : Result {
            override fun toString(): String = reason
        }
    }

    fun <ID> findMissingResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val reported = testReport.reportedIds
        return expectedTestIds.filter { it !in reported }
    }

    fun <ID> findExcessiveResults(expectedTestIds: Collection<ID>, testReport: TestReport<ID>): List<ID> {
        val expected = expectedTestIds.toSet()
        return testReport.reportedIds.filter { it !in expected }
    }

    fun <ID> checkNonEmpty(testReport: TestReport<ID>): Result =
        if (testReport.isEmpty()) {
            Result.Failed("No tests have been found. Test report is empty.")
        } else {
            Result.Passed
        }
}
