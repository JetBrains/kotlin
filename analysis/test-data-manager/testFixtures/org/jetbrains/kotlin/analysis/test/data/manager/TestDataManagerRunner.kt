/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner.COMPACT_LIST_LIMIT
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner.MAX_CONVERGENCE_PASSES
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner.verbose
import org.jetbrains.kotlin.analysis.test.data.manager.filters.*
import org.jetbrains.kotlin.analysis.test.data.manager.listeners.FileUpdateTrackingListener
import org.jetbrains.kotlin.analysis.test.data.manager.listeners.IjLogTestListener
import org.jetbrains.kotlin.analysis.test.data.manager.report.TestDataManagerReport
import org.jetbrains.kotlin.analysis.test.data.manager.report.displayPath
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.*
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import kotlin.uuid.Uuid

/**
 * Validates that there are no conflicts within a list of tests.
 * See README.md for the conflict rule.
 */
internal fun validateConflicts(tests: List<DiscoveredTest>): List<VariantChainConflict> {
    // Work with unique variant chains only - no need to check same chains multiple times
    val uniqueChains = tests.mapTo(linkedSetOf()) { it.variantChain }.toList()

    val conflicts = mutableListOf<VariantChainConflict>()

    for (i in uniqueChains.indices) {
        for (j in i + 1 until uniqueChains.size) {
            val a = uniqueChains[i]
            val b = uniqueChains[j]

            if (a.isEmpty() || b.isEmpty()) continue

            val lastA = a.last()
            val lastB = b.last()

            if (lastA in b) {
                conflicts += VariantChainConflict(
                    chainA = a,
                    chainB = b,
                    conflictingVariant = lastA,
                    reason = "$a writes '$lastA' which $b reads",
                )
            }

            if (lastB in a && lastB != lastA) {
                conflicts += VariantChainConflict(
                    chainA = a,
                    chainB = b,
                    conflictingVariant = lastB,
                    reason = "$b writes '$lastB' which $a reads",
                )
            }
        }
    }

    return conflicts
}

/**
 * Main runner for test data management using JUnit Platform Launcher API.
 * See README.md for grouping and conflict rules.
 *
 * Usage: run by the `checkTestData` and `updateTestData` Gradle tasks, which pass the options below as system properties
 * with the `org.jetbrains.kotlin.testDataManager.options.` prefix:
 * - `mode` - `check` (fail on mismatch) or `update` (update files)
 * - `testDataPath` - comma-separated repository-relative paths to filter tests by (optional)
 * - `testClassPattern` - regex pattern for test class names (optional)
 * - `goldenOnly` - run only golden tests (optional)
 * - `incremental` - run variant tests only for the golden paths that changed (optional, update mode only)
 * - `runId` - the id of the run, naming its report `<runId>.json` (optional, a UUIDv7 by default)
 * - `rerunId` - the id of the earlier run this one reruns; the rotation of old reports keeps its report (optional)
 * - `output` - `verbose` for the progress of every group and pass (optional, compact by default)
 *
 * The tasks also pass `rootDir` (the repository root), `projectDir`, `projectName` and `reportDir`.
 *
 * @see VariantChainComparator
 */
internal object TestDataManagerRunner {
    private const val MAX_CONVERGENCE_PASSES = 10

    /** How many lines of each list (problems, errors, changed files) the compact output prints. */
    private const val COMPACT_LIST_LIMIT = 50

    /**
     * Holds timing measurements collected during test data manager execution.
     */
    private class TimingStats {
        var discoveryTime: Duration = Duration.ZERO
        var groupingTime: Duration = Duration.ZERO
        var executionTime: Duration = Duration.ZERO
    }

    /**
     * Whether to print the progress of every group and pass and the timings, as opposed to the compact default:
     * a header, the counts, one line per problem, error and changed file, and the report path.
     */
    private var verbose: Boolean = false

    /** Prints [message] in the [verbose] output only. */
    private fun detail(message: String) {
        if (verbose) println(message)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        verbose = System.getProperty(TEST_DATA_MANAGER_OPTIONS_OUTPUT)?.lowercase() == "verbose"
        val testDataPath = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_DATA_PATH)
        val testClassPattern = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_CLASS_PATTERN)
        val goldenOnly = System.getProperty(TEST_DATA_MANAGER_OPTIONS_GOLDEN_ONLY)?.toBooleanStrictOrNull() ?: false
        val incremental = System.getProperty(TEST_DATA_MANAGER_OPTIONS_INCREMENTAL)?.toBooleanStrictOrNull() ?: false
        val requestedRunId = System.getProperty(TEST_DATA_MANAGER_OPTIONS_RUN_ID)?.takeIf(String::isNotBlank)
        val runIdError = requestedRunId?.let(TestDataManagerReport::runIdError)
        val runId = requestedRunId?.takeIf { runIdError == null } ?: Uuid.generateV7().toString()
        val mode = TestDataManagerMode.currentModeOrNull
        // Set by the Gradle tasks; without them (a direct launch) the paths are used as given and no report is written
        val rootDir = System.getProperty(TEST_DATA_MANAGER_OPTIONS_ROOT_DIR)?.let(::Path)
        val projectDir = System.getProperty(TEST_DATA_MANAGER_OPTIONS_PROJECT_DIR)?.let(::Path)

        val report = TestDataManagerReport(
            runId = runId,
            project = System.getProperty(TEST_DATA_MANAGER_OPTIONS_PROJECT_NAME),
            mode = mode,
            options = TestDataManagerReport.Options(testDataPath?.let(::splitTestDataPaths), testClassPattern, goldenOnly, incremental),
        )

        val totalTime = measureTime {
            if (verbose) {
                println("Starting test data manager...")
                println("  runId: $runId")
                println("  mode: ${mode ?: "NOT SET"}")
                println("  testDataPath: ${testDataPath ?: "NOT SET"}")
                println("  testClassPattern: ${testClassPattern ?: "NOT SET"}")
                println("  goldenOnly: $goldenOnly")
                println("  incremental: $incremental")
            } else {
                val project = report.project?.let { "$it " }.orEmpty()
                println("Test data manager: $project${mode ?: "mode not set"}, runId $runId")
            }

            when {
                runIdError != null -> {
                    System.err.println("ERROR: $runIdError")
                    report.runErrors += runIdError
                }

                mode == null -> {
                    System.err.println("WARNING: Mode is not set!")
                    System.err.println("Set -D$TEST_DATA_MANAGER_OPTIONS_MODE=check or -D$TEST_DATA_MANAGER_OPTIONS_MODE=update")
                    report.runErrors += "Mode is not set"
                }

                else -> try {
                    val outcome = LauncherFactory.openSession().use { launcherSession ->
                        discoverAndRunTests(
                            testClassPattern,
                            testDataPath,
                            goldenOnly,
                            incremental,
                            rootDir = rootDir,
                            projectDir = projectDir,
                            launcherSession = launcherSession,
                        )
                    }

                    report.record(outcome)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    report.runErrors += e.toString()
                }
            }

            detail("\nTest data management complete.")
        }

        detail("Total time: $totalTime")
        writeReport(report, rootDir)

        // The explicit exit is required to ensure the process terminates after completion
        exitProcess(if (report.status == TestDataManagerReport.Status.PASSED) 0 else 1)
    }

    private fun TestDataManagerReport.record(outcome: RunOutcome) {
        runErrors += outcome.runErrors
        discoveredTests = outcome.totalTests
        passedTests = outcome.passedTests
        failedTests = outcome.failedTests
        skippedTests = outcome.skippedTests
        problems += outcome.problems
        testErrors += outcome.errors
        fileActions += outcome.fileActions
        recoveredTests += outcome.recoveredTests
    }

    /**
     * Writes [report] into the module's report directory passed by Gradle, if any, and prints its path.
     * A failure to write it doesn't hide the result of the run itself.
     */
    private fun writeReport(report: TestDataManagerReport, rootDir: Path?) {
        val reportDirectory = System.getProperty(TEST_DATA_MANAGER_OPTIONS_REPORT_DIR)?.let(::Path) ?: return
        try {
            val rerunId = System.getProperty(TEST_DATA_MANAGER_OPTIONS_RERUN_ID)?.takeIf(String::isNotBlank)
            val reportFile = report.write(reportDirectory, rootDir, keptRunId = rerunId)
            println("Test data manager report: ${reportFile.toAbsolutePath()}")
        } catch (e: Exception) {
            System.err.println("WARNING: failed to write the test data manager report into $reportDirectory: $e")
        }
    }

    /**
     * The result of [discoverAndRunTests].
     *
     * @property testExecutions how many times tests were executed, counting each re-run of the convergence loop
     */
    internal class RunOutcome(
        val totalTests: Int,
        val passedTests: Int,
        val skippedTests: Int,
        val problems: Set<TestDataProblem>,
        val errors: Set<TestError>,
        val fileActions: List<FileAction>,
        val testExecutions: Int,
        val runErrors: List<String> = emptyList(),
        val recoveredTests: List<RecoveredTest> = emptyList(),
    ) {
        val failedTests: Int
            get() = totalTests - passedTests - skippedTests

        /** Whether the run passes; otherwise the runner exits with 1. */
        val isSuccessful: Boolean
            get() = problems.isEmpty() && errors.isEmpty() && runErrors.isEmpty()

        fun withRunErrors(runErrors: List<String>): RunOutcome =
            RunOutcome(totalTests, passedTests, skippedTests, problems, errors, fileActions, testExecutions, runErrors, recoveredTests)

        companion object {
            val EMPTY = RunOutcome(0, 0, 0, emptySet(), emptySet(), emptyList(), 0)
        }
    }

    /**
     * Result of running a test group, containing test data problems and errors.
     *
     * @property finalPassFailedTestIds Test IDs that failed in the final convergence pass.
     *           Used to calculate passed/failed counts - if a test eventually passes after
     *           convergence, it counts as passed (matching standard Gradle behavior).
     */
    private data class RunResult(
        val problems: Set<TestDataProblem>,
        val errors: Set<TestError>,
        val finalPassFailedTestIds: Set<String>,
        val testExecutions: Int,
        val recoveredTests: Collection<RecoveredTest>,
    )

    /**
     * Discovers the managed tests matching the filters and runs them group by group.
     *
     * @param testDataPath the comma-separated `testDataPath` option, resolved against [rootDir] if it is set:
     *                     a path that doesn't exist fails the run, see [resolveTestDataPaths]
     * @param projectDir the module directory, to suggest the repository-relative form of a module-relative path
     * @param additionalFilters discovery filters on top of the options, e.g., to restrict the run to some packages in tests
     */
    context(launcherSession: LauncherSession)
    internal fun discoverAndRunTests(
        testClassPattern: String?,
        testDataPath: String?,
        goldenOnly: Boolean = false,
        incremental: Boolean = false,
        vararg additionalFilters: Filter<*>,
        rootDir: Path? = null,
        projectDir: Path? = null,
    ): RunOutcome {
        val testDataPaths = testDataPath?.let { option ->
            val resolved = rootDir?.let { resolveTestDataPaths(option, it, projectDir) } ?: return@let splitTestDataPaths(option)
            if (resolved.errors.isNotEmpty()) {
                resolved.errors.forEach { System.err.println("ERROR: $it") }
                return RunOutcome.EMPTY.withRunErrors(resolved.errors)
            }

            resolved.paths
        }

        val timingStats = TimingStats()
        val launcher = launcherSession.launcher
        val request = buildDiscoveryRequest(testClassPattern, testDataPaths, goldenOnly, *additionalFilters)

        // Phase 1: Discover tests with variant chains
        (val discoveredTests = value, val discoveryTime = duration) = measureTimedValue {
            val testPlan = launcher.discover(request)
            discoverTests(testPlan)
        }

        timingStats.discoveryTime = discoveryTime
        detail("Discovered ${discoveredTests.size} tests in $discoveryTime")

        if (discoveredTests.isEmpty()) {
            println("No tests found matching criteria")
            return RunOutcome.EMPTY
        }

        // Phase 2: Group by variant chain depth
        (val groupingResult = value, val groupingTime = duration) = measureTimedValue {
            groupByVariantDepth(discoveredTests)
        }

        timingStats.groupingTime = groupingTime

        // Phase 3: Validate no conflicts
        if (groupingResult.conflicts.isNotEmpty()) {
            System.err.println("ERROR: Variant chain conflicts detected:")
            for (conflict in groupingResult.conflicts) {
                System.err.println("  ${conflict.format()}")
            }

            return RunOutcome.EMPTY.withRunErrors(groupingResult.conflicts.map { "Variant chain conflict: ${it.format()}" })
        }

        // Print grouping info
        detail("Grouped into ${groupingResult.groups.size} phases by variant depth in $groupingTime:")
        for (group in groupingResult.groups) {
            detail("  Depth ${group.variantDepth}: ${group.tests.size} tests, variants: ${group.uniqueVariantChains}")
        }

        // Phase 4: Run each group with convergence, aggregate results
        return executeTestGroups(incremental, goldenOnly, groupingResult, launcher, timingStats, discoveredTests)
    }

    context(_: LauncherSession)
    private fun executeTestGroups(
        incremental: Boolean,
        goldenOnly: Boolean,
        groupingResult: GroupingResult,
        launcher: Launcher,
        timingStats: TimingStats,
        discoveredTests: List<DiscoveredTest>,
    ): RunOutcome {
        val allProblems = mutableSetOf<TestDataProblem>()
        val allErrors = mutableSetOf<TestError>()
        val allFinalFailedTestIds = mutableSetOf<String>()
        var allFileActions = emptyList<FileAction>()
        val allRecoveredTests = mutableListOf<RecoveredTest>()
        var testExecutions = 0
        ManagedTestAssertions.recordFileActions = true

        val useIncremental = when {
            !incremental -> false
            TestDataManagerMode.currentMode != TestDataManagerMode.UPDATE -> {
                System.err.println("WARNING: incremental is only effective in update mode (updateTestData), ignoring")
                false
            }

            goldenOnly -> {
                System.err.println("WARNING: incremental has no effect with goldenOnly, ignoring")
                false
            }

            else -> true
        }

        var changedTestDataPaths: Set<String> = emptySet()
        var skippedTests = 0

        for ([index, group] in groupingResult.groups.withIndex()) {
            val isFirstGroup = index == 0

            val effectiveGroup = if (useIncremental && !isFirstGroup) {
                val filtered = group.tests.filter { test ->
                    test.testDataPath == null || changedTestDataPaths.any { changedPath ->
                        Path(changedPath).endsWith(test.testDataPath)
                    }
                }

                if (filtered.isEmpty()) {
                    detail("\n  Skipping group variants: ${group.uniqueVariantChains} (${group.tests.size} tests) — no matching changes")
                    skippedTests += group.tests.size
                    continue
                }

                if (filtered.size < group.tests.size) {
                    detail("\n  Incremental: running ${filtered.size}/${group.tests.size} tests for variants: ${group.uniqueVariantChains}")
                    skippedTests += group.tests.size - filtered.size
                }

                TestGroup(group.variantDepth, filtered)
            } else {
                group
            }

            val groupTime = measureTime {
                val result = runGroupWithConvergence(launcher, effectiveGroup)
                allProblems.addAll(result.problems)
                allErrors.addAll(result.errors)
                allFinalFailedTestIds.addAll(result.finalPassFailedTestIds)
                allRecoveredTests.addAll(result.recoveredTests)
                testExecutions += result.testExecutions
            }

            val groupFileActions = ManagedTestAssertions.drainFileActions()
            // Groups of different depths can write the same file, e.g., `[js]` and `[knm, js]`
            allFileActions = allFileActions.mergedWith(groupFileActions)

            if (useIncremental && isFirstGroup) {
                changedTestDataPaths = groupFileActions.mapTo(HashSet()) { it.testDataPath.toString() }
                if (changedTestDataPaths.isNotEmpty()) {
                    detail("  Incremental: ${changedTestDataPaths.size} test data path(s) changed in first group")
                } else {
                    detail("  Incremental: no changes in first group, skipping all subsequent groups")
                }
            }

            timingStats.executionTime += groupTime
        }

        // Plain test runs in the same JVM, like this module's own tests, must not keep recording
        ManagedTestAssertions.recordFileActions = false

        if (skippedTests > 0) {
            detail("  Incremental: skipped $skippedTests variant tests total")
        }

        // Calculate test counts based on final pass results
        val totalTests = discoveredTests.size
        val failedTests = allFinalFailedTestIds.size
        val passedTests = totalTests - failedTests - skippedTests

        printSummary(allProblems, allErrors, allFileActions, totalTests, passedTests, skippedTests)
        printRecoveredTests(allRecoveredTests)
        printTimingSummary(timingStats)

        return RunOutcome(
            totalTests, passedTests, skippedTests, allProblems, allErrors, allFileActions, testExecutions,
            recoveredTests = allRecoveredTests,
        )
    }

    internal fun buildDiscoveryRequest(
        testClassPattern: String? = null,
        testDataPaths: List<String>? = null,
        goldenOnly: Boolean = false,
        vararg additionalFilters: Filter<*>,
    ): LauncherDiscoveryRequest {
        val builder = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage("org.jetbrains.kotlin"))
            .filters(
                EngineFilter.includeEngines("junit-jupiter"),
                // Only generated tests are included to significantly reduce the number of candidates
                // since anyway this tool is not replacement for the standard `test` command
                ClassNameFilter.includeClassNamePatterns(".*Generated"),
                ManagedTestFilter(goldenOnly),
            )

        if (testDataPaths != null) {
            builder.filters(TestMetadataFilter(testDataPaths))
        }

        if (testClassPattern != null) {
            builder.filters(ClassNameFilter.includeClassNamePatterns(testClassPattern))
        }

        if (additionalFilters.isNotEmpty()) {
            builder.filters(*additionalFilters)
        }

        return builder.build()
    }


    /**
     * Discovers all tests from a test plan and transforms them using the provided function.
     */
    internal fun <T> discoverTests(testPlan: TestPlan, transform: (TestIdentifier) -> T): List<T> {
        val result = mutableListOf<T>()

        for (root in testPlan.roots) {
            for (descriptor in testPlan.getDescendants(root)) {
                if (!descriptor.isTest) continue
                result.add(transform(descriptor))
            }
        }

        return result
    }

    /**
     * Discovers all tests from a test plan and extracts their variant chain information.
     *
     * @param testPlan The JUnit test plan to discover tests from
     * @return List of discovered tests with their variant chain information
     */
    internal fun discoverTests(testPlan: TestPlan): List<DiscoveredTest> = discoverTests(testPlan) { descriptor ->
        DiscoveredTest(
            uniqueId = descriptor.uniqueId,
            displayName = descriptor.displayName,
            variantChain = descriptor.variantChain,
            testDataPath = descriptor.testDataPath,
        )
    }

    /**
     * Groups tests by variant chain depth and validates for conflicts.
     * See README.md for grouping and conflict rules.
     */
    internal fun groupByVariantDepth(tests: List<DiscoveredTest>): GroupingResult {
        val groups = tests
            .groupBy { it.variantChain.size }
            .toSortedMap()
            .map { [depth, testsInGroup] -> TestGroup(depth, testsInGroup) }

        val allConflicts = groups.flatMap { group ->
            validateConflicts(group.tests)
        }

        return GroupingResult(groups, allConflicts)
    }

    /**
     * Builds a discovery request that selects specific tests by their unique IDs.
     */
    private fun buildGroupRequest(testIds: Collection<String>): LauncherDiscoveryRequest {
        return LauncherDiscoveryRequestBuilder.request()
            .selectors(testIds.map { DiscoverySelectors.selectUniqueId(it) })
            .build()
    }

    /**
     * Runs a test group with a convergence loop.
     *
     * Within each variant depth group, tests may update files that other tests depend on.
     * The runner executes passes until no more updates occur (max [MAX_CONVERGENCE_PASSES] passes).
     *
     * All tests in the group run together in a single execution call - JUnit Platform handles
     * parallel execution internally. Non-conflicting subgroups (different last variants) can
     * run in parallel safely.
     *
     * @return Aggregated run result with mismatches and errors from all tests in the group.
     */
    private fun runGroupWithConvergence(launcher: Launcher, group: TestGroup): RunResult {
        val depthStr = if (group.variantDepth == 0) "(golden)" else "depth=${group.variantDepth}"
        detail("\n========================================")
        detail("Running group: $depthStr (${group.tests.size} tests)")
        detail("  Variants: ${group.uniqueVariantChains}")
        detail("========================================")

        return runConvergenceLoop(launcher, group)
    }

    private fun runConvergenceLoop(launcher: Launcher, group: TestGroup): RunResult {
        // The results of the last pass each test ran in: a test that passes on a re-run drops its earlier failure
        val finalProblems = mutableMapOf<String, List<TestDataProblem>>()
        val finalErrors = mutableMapOf<String, List<TestError>>()
        val recoveredTests = mutableMapOf<String, RecoveredTest>()
        var lastPassFailedTestIds: Set<String> = emptySet()
        var lastErrorTestCount = 0
        var lastMismatchedTestCount = 0
        var testIdsToExecute: Collection<String> = group.tests.map { it.uniqueId }

        val projectName: String? = System.getProperty(TEST_DATA_MANAGER_OPTIONS_PROJECT_NAME)

        var testExecutions = 0
        var pass = 1
        while (pass <= MAX_CONVERGENCE_PASSES) {
            detail("\n=== Pass $pass ===")
            val testCount = testIdsToExecute.size
            testExecutions += testCount
            detail("  Tests to execute: $testCount")

            val request = buildGroupRequest(testIdsToExecute)
            val fileTrackingListener = FileUpdateTrackingListener()
            val ijLogListener = if (TestDataManagerMode.isUnderIde) {
                val uniqueVariantChains = group.uniqueVariantChains
                val suiteName = buildString {
                    projectName?.let { append("[$it] ") }
                    append("Running group: ")

                    if (uniqueVariantChains.firstOrNull().isNullOrEmpty()) {
                        append("golden")
                    } else {
                        append(uniqueVariantChains)
                    }

                    append(", pass #$pass ($testCount tests)")
                }

                val idPrefix = "$projectName/$uniqueVariantChains/$pass/"
                IjLogTestListener(suiteName, idPrefix)
            } else {
                null
            }

            val listeners = listOfNotNull(fileTrackingListener, ijLogListener)

            // Execute tests with both listeners
            launcher.execute(request, *listeners.toTypedArray())

            val errorTests = fileTrackingListener.getTestsWithErrors()
            val mismatchesByPath = fileTrackingListener.getMismatchesByPath()

            // Capture failed test IDs from this pass (will keep the last pass's value)
            lastPassFailedTestIds = errorTests

            // Replace the results of the tests re-run in this pass
            val executedTests = fileTrackingListener.getExecutedTests()
            val passProblems = fileTrackingListener.getTestDataProblems()
            val passErrors = fileTrackingListener.getTestErrors()
            for (testId in executedTests) {
                if (testId in passProblems || testId in passErrors) continue
                val earlierProblems = finalProblems[testId].orEmpty()
                val earlierErrors = finalErrors[testId].orEmpty()
                val testName = earlierProblems.firstOrNull()?.testName ?: earlierErrors.firstOrNull()?.testName ?: continue
                recoveredTests[testId] = RecoveredTest(testName, pass, earlierProblems, earlierErrors)
            }

            finalProblems.keys.removeAll(executedTests)
            finalErrors.keys.removeAll(executedTests)
            finalProblems.putAll(fileTrackingListener.getTestDataProblems())
            finalErrors.putAll(fileTrackingListener.getTestErrors())

            val errorTestCount = errorTests.size
            val mismatchedTestCount = mismatchesByPath.size
            detail("  Tests with errors: $errorTestCount (including $mismatchedTestCount mismatches)")

            // A read-only check changes no file, so a re-run would only repeat the same results
            val isReadOnly = TestDataManagerMode.isExplicitCheck
            if (isReadOnly || lastErrorTestCount == errorTestCount && lastMismatchedTestCount == mismatchedTestCount) {
                detail("  Convergence reached.")
                return RunResult(
                    problems = finalProblems.values.flatten().toSet(),
                    errors = finalErrors.values.flatten().toSet(),
                    finalPassFailedTestIds = lastPassFailedTestIds,
                    testExecutions = testExecutions,
                    recoveredTests = recoveredTests.values,
                )
            } else {
                lastErrorTestCount = errorTestCount
                lastMismatchedTestCount = mismatchedTestCount
            }

            pass++

            if (pass > MAX_CONVERGENCE_PASSES) {
                System.err.println("WARNING: Max passes ($MAX_CONVERGENCE_PASSES) reached without convergence")
            }

            testIdsToExecute = lastPassFailedTestIds
        }

        return RunResult(
            problems = finalProblems.values.flatten().toSet(),
            errors = finalErrors.values.flatten().toSet(),
            finalPassFailedTestIds = lastPassFailedTestIds,
            testExecutions = testExecutions,
            recoveredTests = recoveredTests.values,
        )
    }

    /** Prints the counts and the lists of the run; `internal` for the tests of the compact output. */
    internal fun printSummary(
        problems: Set<TestDataProblem>,
        errors: Set<TestError>,
        fileActions: List<FileAction>,
        totalTests: Int,
        passedTests: Int,
        skippedTests: Int,
    ) {
        detail("\n========================================")
        detail("SUMMARY")
        detail("========================================")

        val failedTests = totalTests - passedTests - skippedTests
        val skippedSuffix = if (skippedTests > 0) ", $skippedTests skipped (incremental)" else ""
        println("$totalTests tests completed, $passedTests passed, $failedTests failed$skippedSuffix")

        if (fileActions.isNotEmpty()) {
            detail("\n=== Changed files (${fileActions.size}) ===")
            printCapped(fileActions.sortedBy { it.file }) { action -> "${action.kind} ${action.file.displayPath}" }
        }

        if (problems.isNotEmpty()) {
            val files = problems.mapTo(HashSet()) { it.file }
            detail("\n=== Test data problems (${files.size} files) ===")
            printCapped(problems.sortedWith(compareBy({ it.file }, { it.testName }))) { problem ->
                "${problem.kind} ${problem.file.displayPath} (${problem.testName})"
            }
            // In UPDATE, mismatches come only from assertions that can't update their files
            if (TestDataManagerMode.currentMode == TestDataManagerMode.CHECK) {
                detail("")
                println("Run updateTestData with the same options to update them.")
            }
        }

        if (errors.isNotEmpty()) {
            detail("\n=== Errors (${errors.size} tests) ===")
            printCapped(errors.sortedBy { it.testName }) { error -> "${if (verbose) "" else "ERROR "}${error.testName} - ${error.exception}" }
        }

        if (problems.isEmpty() && errors.isEmpty()) {
            detail("\nAll tests passed!")
        }
    }

    /**
     * Lists the tests that passed only on a re-run, with what they failed with before: they don't fail the run, but may
     * be flaky.
     */
    private fun printRecoveredTests(recoveredTests: List<RecoveredTest>) {
        if (recoveredTests.isEmpty()) return
        detail("\n=== Passed only on a re-run (${recoveredTests.size} tests) ===")
        printCapped(recoveredTests.sortedBy { it.testName }) { test ->
            "${if (verbose) "" else "RECOVERED "}${test.testName} (pass ${test.passedInPass}) - ${test.failureSummary}"
        }
    }

    /**
     * Prints a line per element, only the first [COMPACT_LIST_LIMIT] of them in the compact output: a mass change would
     * flood the console, while the report has them all.
     */
    private inline fun <T> printCapped(elements: List<T>, line: (T) -> String) {
        val shown = if (verbose) elements else elements.take(COMPACT_LIST_LIMIT)
        shown.forEach { println(line(it)) }
        if (shown.size < elements.size) {
            println("... and ${elements.size - shown.size} more, see the report")
        }
    }

    private val RecoveredTest.failureSummary: String
        get() = errors.firstOrNull()?.exception ?: problems.first().let { "${it.kind} ${it.file.displayPath}" }

    /** The path relative to the repository root when it is inside, for shorter output. */
    private val Path.displayPath: String
        get() = displayPath(System.getProperty(TEST_DATA_MANAGER_OPTIONS_ROOT_DIR)?.let(::Path))

    private fun printTimingSummary(timingStats: TimingStats) {
        detail("\n========================================")
        detail("TIMING SUMMARY")
        detail("========================================")
        detail("  Discovery:  ${timingStats.discoveryTime}")
        detail("  Grouping:   ${timingStats.groupingTime}")
        detail("  Execution:  ${timingStats.executionTime}")
    }
}
