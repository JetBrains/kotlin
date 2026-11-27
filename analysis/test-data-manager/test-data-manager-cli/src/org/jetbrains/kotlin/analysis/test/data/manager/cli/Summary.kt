/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.analysis.test.data.manager.FileActionKind
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataProblemKind
import org.jetbrains.kotlin.analysis.test.data.manager.report.RunStatus
import org.jetbrains.kotlin.analysis.test.data.manager.report.TestCounts
import java.nio.file.Path

/** The status of a module in the summary: that of its report, or why it has none. */
internal enum class ModuleStatus { PASSED, FAILED, ERROR, MISSING, SKIPPED }

/** The index of a run: its status, the totals and counts, and the module reports holding the details. */
@Serializable
internal data class Summary(
    val runId: String,
    val mode: TestDataManagerMode,
    val status: RunStatus,
    val interrupted: Boolean,
    val runErrors: List<String>,
    val gradleFailures: List<String>,
    val compilerErrors: List<String>,
    val tests: TestCounts,
    val counts: Counts,
    val modules: List<ModuleEntry>,
    val gradleLog: String?,
    val summary: String,
) {
    /** The entries of the module reports by kind. */
    @Serializable
    data class Counts(
        val problems: Map<TestDataProblemKind, Int>,
        val testErrors: Int,
        val recoveredTests: Int,
        val fileActions: Map<FileActionKind, Int>,
    )

    @Serializable
    data class ModuleEntry(
        val project: String,
        val status: ModuleStatus,
        val tests: TestCounts?,
        val report: String?,
        val buildFile: String?,
    )
}

/**
 * What a run did, from which [summarize] builds its summary.
 *
 * @property hasFilters whether the tests are filtered, by `testDataPath` entries or by a class pattern
 * @property gradle how the last build of the run ended, if one was started and waited for
 * @property gradleOutput the text of [gradleLog]
 * @property reports the module reports of the run, by project
 * @property buildFiles the repository-relative build files of the modules that ran and of the skipped ones, by project
 */
internal class RunResult(
    val mode: TestDataManagerMode,
    val runId: String,
    val plan: RunPlan,
    val hasFilters: Boolean,
    val gradle: GradleOutcome?,
    val gradleOutput: String,
    val interrupted: Boolean,
    val reports: Map<String, LoadedReport>,
    val buildFiles: Map<String, String>,
    val gradleLog: Path,
    val summaryFile: Path,
) {
    /** The entries of the reports of the modules that ran. */
    val merged: MergedEntries = merge(plan.modules, reports)
}

/** A Kotlin (`e: ...`) or Java (`File.java:12: error: ...`) compiler error in the Gradle output. */
private val COMPILER_ERROR = Regex("""^(?:e: .*|.*\.java:\d+: error: .*)$""", RegexOption.MULTILINE)

/** The most compiler errors in the summary; the rest is in the Gradle log. */
private const val MAX_COMPILER_ERRORS = 20

/** How many build files the error for filters matching no test names, see [noMatchError]. */
internal const val MAX_NAMED_BUILD_FILES = 3

/** The compiler errors in [gradleOutput], the first [MAX_COMPILER_ERRORS] of them. */
internal fun compilerErrors(gradleOutput: String): List<String> {
    val errors = COMPILER_ERROR.findAll(gradleOutput).map { it.value }.toList()
    if (errors.size <= MAX_COMPILER_ERRORS) return errors
    return errors.take(MAX_COMPILER_ERRORS) + "... ${errors.size - MAX_COMPILER_ERRORS} more compiler errors"
}

/**
 * The error for filters that match no managed test in any module. A module finds its managed tests only in the packages it
 * declares, so the error names the [buildFiles] of the modules that ran, up to [MAX_NAMED_BUILD_FILES] of them; otherwise
 * it points to the summary, which has the build file of each module.
 */
internal fun noMatchError(buildFiles: List<String>): String {
    val error = "No managed test in any module matches the filters"
    if (buildFiles.isEmpty()) return error
    val where = if (buildFiles.size <= MAX_NAMED_BUILD_FILES) buildFiles.joinToString(", ")
    else "the build file of each module that ran (`buildFile` of the modules in the summary)"
    return "$error. A module finds its managed tests only in the packages of `testDataManager { testPackages }` " +
            "(`org.jetbrains.kotlin` by default): if the tests exist, check their packages there, in $where"
}

/**
 * The error for the `testDataPath` entry [entry] that no test of the modules that ran is in or under. A module runs for any
 * path in the test data it declares, so it is its runner that finds no test of such a path.
 */
internal fun unmatchedPathError(entry: String): String =
    "testDataPath '$entry' matches no managed test in the modules that ran: a test is selected by its test data file " +
            "(`foo.kt`), not by its output (`foo.txt`); a new test data file needs `./gradlew generateTests` first (the tests " +
            "a module generates into `build` are updated by its build)"

/**
 * The summary of [run]. On top of the module reports, it has what a single module can't see: a module that wrote no
 * report, filters that match no test in any module, and the failures of Gradle other than a failing module report.
 */
internal fun summarize(run: RunResult): Summary {
    val modules = run.plan.modules
    // An interrupted build fails for being interrupted
    val failedBuild = run.gradle?.takeIf { it.failed && !run.interrupted }
    // A task whose report failed fails for it: its failure adds nothing to the report
    val reportedTasks = run.reports.keys.mapTo(mutableSetOf()) { "$it:${run.mode.taskName}" }
    val gradleFailures = failedBuild?.failures.orEmpty().filter { it.taskPath !in reportedTasks }.map { it.text }
    val tests = modules.mapNotNull { run.reports[it]?.content?.tests }.fold(TestCounts.ZERO, TestCounts::plus)
    val runErrors = runErrors(run, tests, gradleFailed = failedBuild != null, gradleFailures)
    val problems = run.merged.problems
    val testErrors = run.merged.testErrors

    fun moduleEntry(module: String, status: ModuleStatus, report: LoadedReport?) = Summary.ModuleEntry(
        project = module,
        status = status,
        tests = report?.content?.tests,
        report = report?.file,
        buildFile = run.buildFiles[module],
    )

    return Summary(
        runId = run.runId,
        mode = run.mode,
        status = when {
            runErrors.isNotEmpty() -> RunStatus.ERROR
            problems.isNotEmpty() || testErrors.isNotEmpty() -> RunStatus.FAILED
            else -> RunStatus.PASSED
        },
        interrupted = run.interrupted,
        runErrors = runErrors,
        gradleFailures = gradleFailures,
        compilerErrors = if (failedBuild != null) compilerErrors(run.gradleOutput) else emptyList(),
        tests = tests,
        counts = Summary.Counts(
            problems = problems.groupingBy { it.kind }.eachCount().toSortedMap(compareBy { it.name }),
            testErrors = testErrors.size,
            recoveredTests = run.merged.recoveredTests.size,
            fileActions = run.merged.fileActions.groupingBy { it.kind }.eachCount().toSortedMap(compareBy { it.name }),
        ),
        modules = modules.map { module ->
            val report = run.reports[module]
            moduleEntry(module, report?.content?.status.toModuleStatus(), report)
        } + run.plan.skipped.map { moduleEntry(it, ModuleStatus.SKIPPED, report = null) },
        gradleLog = run.gradleLog.toString(),
        summary = run.summaryFile.toString(),
    )
}

/** The status of a module that ran, from the status of its report, `null` if it has none. */
private fun RunStatus?.toModuleStatus(): ModuleStatus = when (this) {
    RunStatus.PASSED -> ModuleStatus.PASSED
    RunStatus.FAILED -> ModuleStatus.FAILED
    RunStatus.ERROR -> ModuleStatus.ERROR
    null -> ModuleStatus.MISSING
}

/**
 * The errors of [run] itself: those of its plan, of the runners of its modules, and what only the wrapper sees, see
 * [summarize].
 *
 * @param tests the tests of all modules
 * @param gradleFailed whether Gradle failed, not counting an interrupted build
 * @param gradleFailures the failures of Gradle, without those of the tasks whose report failed
 */
private fun runErrors(run: RunResult, tests: TestCounts, gradleFailed: Boolean, gradleFailures: List<String>): List<String> {
    val modules = run.plan.modules
    val reports = modules.mapNotNull { run.reports[it]?.content }
    val missing = modules.filter { it !in run.reports }
    val errors = run.plan.errors.toMutableList()
    if (run.interrupted) {
        errors += "Interrupted before Gradle finished; the modules without a report didn't finish"
    }

    for (module in modules) {
        run.reports[module]?.content?.runErrors?.mapTo(errors) { "$module: $it" }
    }

    if (missing.isNotEmpty() && gradleFailures.isNotEmpty()) {
        errors += "No report from ${missing.joinToString(", ")}, see the Gradle failures"
    } else if (!run.interrupted) {
        errors += missing.map { "$it: no report for this run; the task didn't run or its JVM crashed, see the Gradle log" }
    }

    val noMatch = run.hasFilters && tests.discovered == 0 && errors.isEmpty()
    // A module without a report may have had the tests of a path the others have none of
    if (missing.isEmpty() && reports.isNotEmpty()) {
        val unmatched = run.plan.testDataPaths.filter { entry -> reports.all { entry in it.unmatchedTestDataPaths } }
        errors += unmatched.map(::unmatchedPathError)
    }

    if (noMatch) errors += noMatchError(modules.mapNotNull { run.buildFiles[it] })
    if (gradleFailed && errors.isEmpty() && run.merged.problems.isEmpty() && run.merged.testErrors.isEmpty()) {
        errors += "Gradle failed without a failing report, see the Gradle log"
    }

    return errors
}
