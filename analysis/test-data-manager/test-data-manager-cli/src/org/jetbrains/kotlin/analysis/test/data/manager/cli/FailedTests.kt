/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

/**
 * What `--rerun` takes from a run: the [testDataPaths] of the tests that failed in it, with a problem or a test error,
 * and the [errors]: what failed there and can't be rerun.
 */
internal class FailedTests(val testDataPaths: List<String>, val errors: List<String>)

/** The tests that failed in the run [runId], from its summary and its module reports. */
internal fun RunDirectory.failedTests(runId: String): FailedTests {
    val summary = readSummary(runId) ?: return FailedTests(
        testDataPaths = emptyList(),
        errors = listOf("No summary of the run '$runId' to rerun: ${summaryFile(runId)}; the $KEPT_RUNS most recent runs are kept"),
    )

    val paths = sortedSetOf<String>()
    val errors = mutableListOf<String>()
    for (module in summary.modules) {
        val project = module.project
        if (module.status == ModuleStatus.PASSED || module.status == ModuleStatus.SKIPPED) continue
        val reportFile = module.report
        if (reportFile == null) {
            errors += "$project: no report in the run '$runId', so its failures are unknown"
            continue
        }

        val report = readReport(reportFile)
        if (report == null) {
            errors += "$project: the report of the run '$runId' is gone: $reportFile"
            continue
        }

        report.runErrors.firstOrNull()?.let {
            errors += "$project: the run '$runId' failed there with run errors, so its failures are unknown: $it"
        }

        val failures = report.problems.map { (test, testDataPath) -> test to testDataPath } +
                report.testErrors.map { (test, testDataPath) -> test to testDataPath }
        for ([test, testDataPath] in failures) {
            if (!testDataPath.isNullOrEmpty()) {
                paths += testDataPath
            } else {
                errors += "$project: $test failed without a test data path, so it can't be rerun"
            }
        }
    }

    if (paths.isEmpty() && errors.isEmpty()) errors += "The run '$runId' has no failed tests to rerun"
    return FailedTests(paths.toList(), errors)
}
