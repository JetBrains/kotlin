/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner.MAX_CONVERGENCE_PASSES
import org.jetbrains.kotlin.analysis.test.data.manager.filters.ManagedTestFilter
import org.jetbrains.kotlin.analysis.test.data.manager.filters.TestMetadataFilter
import org.jetbrains.kotlin.analysis.test.data.manager.filters.testDataPath
import org.jetbrains.kotlin.analysis.test.data.manager.filters.variantChain
import org.jetbrains.kotlin.analysis.test.data.manager.listeners.FileUpdateTrackingListener
import org.jetbrains.kotlin.analysis.test.data.manager.listeners.IjLogTestListener
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.*
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.io.path.Path
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

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
 * Usage: Run via Gradle's `javaexec` with system property
 * `-Dkotlin.test.data.manager.mode=check|update`
 *
 * System properties:
 * - `kotlin.test.data.manager.mode` - Mode: `check` (fail on mismatch) or `update` (update files)
 * - `testDataPath` - Filter tests to only those matching this path (optional)
 * - `testClassPattern` - Regex pattern for test class names (optional, default ".*")
 *
 * @see VariantChainComparator
 */
internal object TestDataManagerRunner {
    private const val MAX_CONVERGENCE_PASSES = 10

    /**
     * Holds timing measurements collected during test data manager execution.
     */
    private class TimingStats {
        var discoveryTime: Duration = Duration.ZERO
        var groupingTime: Duration = Duration.ZERO
        var executionTime: Duration = Duration.ZERO
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val totalTime = measureTime {
            val testDataPath = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_DATA_PATH)
            val testClassPattern = System.getProperty(TEST_DATA_MANAGER_OPTIONS_TEST_CLASS_PATTERN)
            val goldenOnly = System.getProperty(TEST_DATA_MANAGER_OPTIONS_GOLDEN_ONLY)?.toBooleanStrictOrNull() ?: false
            val incremental = System.getProperty(TEST_DATA_MANAGER_OPTIONS_INCREMENTAL)?.toBooleanStrictOrNull() ?: false
            val mode = TestDataManagerMode.currentModeOrNull

            println("Starting test data manager...")
            println("  mode: ${mode ?: "NOT SET"}")
            println("  testDataPath: ${testDataPath ?: "NOT SET"}")
            println("  testClassPattern: ${testClassPattern ?: "NOT SET"}")
            println("  goldenOnly: $goldenOnly")
            println("  incremental: $incremental")

            if (mode == null) {
                System.err.println("WARNING: Mode is not set!")
                System.err.println("Set -D$TEST_DATA_MANAGER_OPTIONS_MODE=check or -D$TEST_DATA_MANAGER_OPTIONS_MODE=update")
                exitProcess(1)
            }

            LauncherFactory.openSession().use { launcherSession ->
                context(launcherSession) {
                    discoverAndRunTests(testClassPattern, testDataPath, goldenOnly, incremental)
                }
            }

            println("\nTest data management complete.")
        }

        println("Total time: $totalTime")

        // The explicit exit is required to ensure the process terminates after completion
        exitProcess(0)
    }

    /**
     * Result of running a test group, containing mismatches and errors.
     *
     * @property finalPassFailedTestIds Test IDs that failed in the final convergence pass.
     *           Used to calculate passed/failed counts - if a test eventually passes after
     *           convergence, it counts as passed (matching standard Gradle behavior).
     */
    private data class RunResult(
        val mismatchesByPath: Map<String, Set<TestVariantChain>>,
        val errors: Set<TestError>,
        val finalPassFailedTestIds: Set<String>,
    )

    context(launcherSession: LauncherSession)
    private fun discoverAndRunTests(
        testClassPattern: String?,
        testDataPath: String?,
        goldenOnly: Boolean = false,
        incremental: Boolean = false,
    ) {
        val timingStats = TimingStats()
        val launcher = launcherSession.launcher
        val request = buildDiscoveryRequest(testClassPattern, testDataPath, goldenOnly)

        // Phase 1: Discover tests with variant chains
        val (discoveredTests, discoveryTime) = measureTimedValue {
            val testPlan = launcher.discover(request)
            discoverTests(testPlan)
        }

        timingStats.discoveryTime = discoveryTime
        println("Discovered ${discoveredTests.size} tests in $discoveryTime")

        if (discoveredTests.isEmpty()) {
            println("No tests found matching criteria")
            return
        }

        // Phase 2: Group by variant chain depth
        val (groupingResult, groupingTime) = measureTimedValue {
            groupByVariantDepth(discoveredTests)
        }

        timingStats.groupingTime = groupingTime

        // Phase 3: Validate no conflicts
        if (groupingResult.conflicts.isNotEmpty()) {
            val message = buildString {
                appendLine("Variant chain conflicts detected:")
                for (conflict in groupingResult.conflicts) {
                    appendLine("  ${conflict.format()}")
                }
            }

            throw IllegalStateException(message)
        }

        // Print grouping info
        println("Grouped into ${groupingResult.groups.size} phases by variant depth in $groupingTime:")
        for (group in groupingResult.groups) {
            println("  Depth ${group.variantDepth}: ${group.tests.size} tests, variants: ${group.uniqueVariantChains}")
        }

        // Phase 4: Run each group with convergence, aggregate results
        executeTestGroups(incremental, goldenOnly, groupingResult, launcher, timingStats, discoveredTests)
    }

    context(_: LauncherSession)
    private fun executeTestGroups(
        incremental: Boolean,
        goldenOnly: Boolean,
        groupingResult: GroupingResult,
        launcher: Launcher,
        timingStats: TimingStats,
        discoveredTests: List<DiscoveredTest>,
    ) {
        val allMismatches = mutableMapOf<String, MutableSet<TestVariantChain>>()
        val allErrors = mutableSetOf<TestError>()
        val allFinalFailedTestIds = mutableSetOf<String>()

        val useIncremental = when {
            !incremental -> false
            TestDataManagerMode.currentMode != TestDataManagerMode.UPDATE -> {
                System.err.println("WARNING: --incremental is only effective with --mode=update, ignoring")
                false
            }

            goldenOnly -> {
                System.err.println("WARNING: --incremental is useless with --golden-only, ignoring")
                false
            }

            else -> true
        }

        var changedTestDataPaths: Set<String> = emptySet()
        var skippedTests = 0

        for ((index, group) in groupingResult.groups.withIndex()) {
            val isFirstGroup = index == 0

            if (useIncremental && isFirstGroup) {
                ManagedTestAssertions.trackUpdatedPaths = true
            }

            val effectiveGroup = if (useIncremental && !isFirstGroup) {
                val filtered = group.tests.filter { test ->
                    test.testDataPath == null || changedTestDataPaths.any { changedPath ->
                        Path(changedPath).endsWith(test.testDataPath)
                    }
                }

                if (filtered.isEmpty()) {
                    println("\n  Skipping group variants: ${group.uniqueVariantChains} (${group.tests.size} tests) — no matching changes")
                    skippedTests += group.tests.size
                    continue
                }

                if (filtered.size < group.tests.size) {
                    println("\n  Incremental: running ${filtered.size}/${group.tests.size} tests for variants: ${group.uniqueVariantChains}")
                }

                TestGroup(group.variantDepth, filtered)
            } else {
                group
            }

            val groupTime = measureTime {
                val result = runGroupWithConvergence(launcher, effectiveGroup)
                result.mismatchesByPath.forEach { (path, variantChains) ->
                    allMismatches.getOrPut(path) { mutableSetOf() }.addAll(variantChains)
                }

                allErrors.addAll(result.errors)
                allFinalFailedTestIds.addAll(result.finalPassFailedTestIds)
            }

            if (useIncremental && isFirstGroup) {
                ManagedTestAssertions.trackUpdatedPaths = false
                changedTestDataPaths = ManagedTestAssertions.drainUpdatedTestDataPaths()
                if (changedTestDataPaths.isNotEmpty()) {
                    println("  Incremental: ${changedTestDataPaths.size} test data path(s) changed in first group")
                } else {
                    println("  Incremental: no changes in first group, skipping all subsequent groups")
                }
            }

            timingStats.executionTime += groupTime
        }

        if (skippedTests > 0) {
            println("  Incremental: skipped $skippedTests variant tests total")
        }

        // Calculate test counts based on final pass results
        val totalTests = discoveredTests.size
        val failedTests = allFinalFailedTestIds.size
        val passedTests = totalTests - failedTests

        // Collect all unique variant chain configurations for summary
        val allVariantChains: Set<TestVariantChain> = groupingResult.groups
            .flatMap { it.uniqueVariantChains }
            .toSet()

        // Print summary and exit with appropriate code
        printSummary(allMismatches, allVariantChains, allErrors, totalTests, passedTests)
        printTimingSummary(timingStats)

        if (allMismatches.isNotEmpty() || allErrors.isNotEmpty()) {
            exitProcess(1)
        }
    }

    internal fun buildDiscoveryRequest(
        testClassPattern: String? = null,
        testDataPath: String? = null,
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

        if (testDataPath != null) {
            val includePaths = testDataPath.split(",").map(String::trim).filter(String::isNotEmpty)
            builder.filters(TestMetadataFilter(includePaths))
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
            .map { (depth, testsInGroup) -> TestGroup(depth, testsInGroup) }

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
        println("\n========================================")
        println("Running group: $depthStr (${group.tests.size} tests)")
        println("  Variants: ${group.uniqueVariantChains}")
        println("========================================")

        return runConvergenceLoop(launcher, group)
    }

    private fun runConvergenceLoop(launcher: Launcher, group: TestGroup): RunResult {
        val aggregatedMismatches = mutableMapOf<String, MutableSet<TestVariantChain>>()
        val aggregatedErrors = mutableSetOf<TestError>()
        var lastPassFailedTestIds: Set<String> = emptySet()
        var lastErrorTestCount = 0
        var lastMismatchedTestCount = 0
        var testIdsToExecute: Collection<String> = group.tests.map { it.uniqueId }

        val projectName: String? = System.getProperty(TEST_DATA_MANAGER_OPTIONS_PROJECT_NAME)

        var pass = 1
        while (pass <= MAX_CONVERGENCE_PASSES) {
            println("\n=== Pass $pass ===")
            val testCount = testIdsToExecute.size
            println("  Tests to execute: $testCount")

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

            // Aggregate mismatches and errors from this pass
            mismatchesByPath.forEach { (path, prefixSets) ->
                aggregatedMismatches.getOrPut(path) { mutableSetOf() }.addAll(prefixSets)
            }

            aggregatedErrors.addAll(fileTrackingListener.getTestErrors())

            val errorTestCount = errorTests.size
            val mismatchedTestCount = mismatchesByPath.size
            println("  Tests with errors: $errorTestCount (including $mismatchedTestCount mismatches)")

            if (lastErrorTestCount == errorTestCount && lastMismatchedTestCount == mismatchedTestCount) {
                println("  Convergence reached.")
                return RunResult(aggregatedMismatches, aggregatedErrors, lastPassFailedTestIds)
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

        return RunResult(aggregatedMismatches, aggregatedErrors, lastPassFailedTestIds)
    }

    private fun printSummary(
        mismatchesByPath: Map<String, Set<TestVariantChain>>,
        allVariantChains: Set<TestVariantChain>,
        errors: Set<TestError>,
        totalTests: Int,
        passedTests: Int,
    ) {
        println("\n========================================")
        println("SUMMARY")
        println("========================================")

        val failedTests = totalTests - passedTests
        println("$totalTests tests completed, $passedTests passed, $failedTests failed")

        if (mismatchesByPath.isNotEmpty()) {
            println("\n=== Mismatches (${mismatchesByPath.size} test data paths) ===")
            for ((path, failedVariants) in mismatchesByPath.entries.sortedBy { it.key }) {
                val variantSuffix = formatVariantSuffix(failedVariants, allVariantChains)
                println("$path$variantSuffix")
            }
            println("\nRun with --mode=update to fix these.")
        }

        if (errors.isNotEmpty()) {
            println("\n=== Errors (${errors.size} tests) ===")
            for (error in errors.sortedBy { it.testName }) {
                println("${error.testName} - ${error.exception}")
            }
        }

        if (mismatchesByPath.isEmpty() && errors.isEmpty()) {
            println("\nAll tests passed!")
        }
    }

    private fun formatVariantSuffix(
        failedVariants: Set<TestVariantChain>,
        allVariantChains: Set<TestVariantChain>,
    ): String {
        // If all variant configurations failed, don't show the suffix
        if (failedVariants.size >= allVariantChains.size) {
            return ""
        }

        // Use the last variant as the display name for each failed configuration
        val names = failedVariants
            .filter { it.isNotEmpty() }
            .map { it.last() }
            .sorted()

        return if (names.isNotEmpty()) " (${names.joinToString(", ")})" else ""
    }

    private fun printTimingSummary(timingStats: TimingStats) {
        println("\n========================================")
        println("TIMING SUMMARY")
        println("========================================")
        println("  Discovery:  ${timingStats.discoveryTime}")
        println("  Grouping:   ${timingStats.groupingTime}")
        println("  Execution:  ${timingStats.executionTime}")
    }
}
