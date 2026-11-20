/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.listeners

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataAssertionError
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataProblem
import org.jetbrains.kotlin.analysis.test.data.manager.TestError
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.filters.TestMetadataPath
import org.jetbrains.kotlin.analysis.test.data.manager.filters.formatTestName
import org.jetbrains.kotlin.analysis.test.data.manager.filters.testDataPath
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import org.opentest4j.MultipleFailuresError
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

/**
 * JUnit Platform listener that tracks which tests updated files or had mismatches.
 *
 * Used by [org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner] to determine which tests need re-running
 * in the convergence loop (UPDATE mode) or to report mismatches (CHECK mode).
 *
 * The results are keyed by the test unique ID, so the runner can replace the results of a test re-run in a later pass.
 *
 * A failed container, like a test class whose `@BeforeAll` throws, keeps its tests that hadn't finished or been skipped
 * from running: each of them gets the container's failure as its error, so it counts as failed and an update re-runs
 * it. A container that failed with none of them, like on an `@AfterAll`, gets the error itself, without a test data
 * path: a re-run by the test data path of its class would select more tests than the run did.
 *
 * @param variantChains the variant chains of the tests by unique ID, from the discovery: reading one when a test fails
 *                      would instantiate its class again, which may be what failed
 */
internal class FileUpdateTrackingListener(
    private val variantChains: Map<String, TestVariantChain> = emptyMap(),
) : TestExecutionListener {
    /** The plan being executed, to find the tests a failed container kept from running. */
    @Volatile
    private var testPlan: TestPlan? = null

    /**
     * Unique IDs of all tests that finished, successfully or not.
     */
    private val executedTests = ConcurrentHashMap.newKeySet<String>()

    /**
     * Unique IDs of the tests that finished aborted, like on a failed assumption.
     */
    private val abortedTests = ConcurrentHashMap.newKeySet<String>()

    /**
     * Unique IDs of the tests that failed, or didn't run because their container failed.
     */
    private val testsWithErrors = ConcurrentHashMap.newKeySet<String>()

    /**
     * Unique IDs of the skipped tests and containers, and of the aborted containers: their descendants aren't reported.
     */
    private val skippedIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Map: testDataPath -> set of prefix lists that failed for this path.
     * Used for CHECK mode summary.
     */
    private val mismatchesByPath = ConcurrentHashMap<TestMetadataPath, MutableSet<TestVariantChain>>()

    /**
     * Test data files that didn't match, by test unique ID.
     */
    private val testDataProblems = ConcurrentHashMap<String, List<TestDataProblem>>()

    /**
     * Other test failures, by test unique ID, or by container unique ID for the failures of a container.
     */
    private val testErrors = ConcurrentHashMap<String, List<TestError>>()

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        this.testPlan = testPlan
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
        skippedIds.add(testIdentifier.uniqueId)
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (!testIdentifier.isTest) {
            when (testExecutionResult.status) {
                TestExecutionResult.Status.FAILED ->
                    testExecutionResult.throwable.orElse(null)?.let { recordContainerFailure(testIdentifier, it) }

                // Like a skipped container, e.g., on a failed assumption in `@BeforeAll`
                TestExecutionResult.Status.ABORTED -> skippedIds.add(testIdentifier.uniqueId)
                else -> {}
            }

            return
        }

        executedTests.add(testIdentifier.uniqueId)
        if (testExecutionResult.status == TestExecutionResult.Status.ABORTED) {
            abortedTests.add(testIdentifier.uniqueId)
        }

        // Track failures
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) {
            testsWithErrors.add(testIdentifier.uniqueId)

            val throwable = testExecutionResult.throwable.orElse(null) ?: return
            // Several failures of one test, e.g., from `JUnit5Assertions.failAll`, are classified one by one
            val failures = (throwable as? MultipleFailuresError)?.failures?.takeIf { it.isNotEmpty() } ?: listOf(throwable)
            for (failure in failures) {
                val problem = testDataProblemOrNull(testIdentifier, failure)
                if (problem != null) {
                    testDataProblems.merge(testIdentifier.uniqueId, listOf(problem), List<TestDataProblem>::plus)
                    val testDataPath = testIdentifier.testDataPath
                    if (testDataPath != null) {
                        mismatchesByPath
                            .getOrPut(testDataPath) { ConcurrentHashMap.newKeySet() }
                            .add(testIdentifier.knownVariantChain)
                    }
                } else {
                    // Real error
                    recordError(testIdentifier, testIdentifier.testDataPath, testIdentifier.knownVariantChain, failure)
                }
            }
        }
    }

    /**
     * Records the failure of [container] for its tests that neither finished nor were skipped, or for [container]
     * itself if there are none. The descendants of a container finish before it, so the unfinished ones never ran;
     * those blocked by a nested container already have its failure.
     */
    private fun recordContainerFailure(container: TestIdentifier, throwable: Throwable) {
        val plan = testPlan
        val descendants = plan?.getDescendants(container).orEmpty()
        val blockedTests = descendants.filter {
            it.isTest && it.uniqueId !in executedTests && it.uniqueId !in testsWithErrors && plan?.isSkipped(it) != true
        }
        for (test in blockedTests) {
            testsWithErrors.add(test.uniqueId)
            recordError(test, test.testDataPath, test.knownVariantChain, throwable)
        }

        if (blockedTests.isEmpty()) {
            val variantChain = descendants.firstNotNullOfOrNull { variantChains[it.uniqueId] }.orEmpty()
            recordError(container, testDataPath = null, variantChain, throwable)
        }
    }

    private fun TestPlan.isSkipped(test: TestIdentifier): Boolean =
        generateSequence(test) { getParent(it).orElse(null) }.any { it.uniqueId in skippedIds }

    private fun recordError(identifier: TestIdentifier, testDataPath: TestMetadataPath?, variantChain: TestVariantChain, failure: Throwable) {
        val error = TestError(
            testName = identifier.formatTestName(),
            testDataPath = testDataPath,
            variantChain = variantChain,
            exceptionClass = failure.javaClass.name,
            message = failure.message,
            stackTrace = identifier.topStackFrames(failure),
        )
        testErrors.merge(identifier.uniqueId, listOf(error), List<TestError>::plus)
    }

    private val TestIdentifier.knownVariantChain: TestVariantChain
        get() = variantChains[uniqueId].orEmpty()

    /**
     * The test data problem [throwable] reports: a [TestDataAssertionError], or a comparison with an expected file
     * from another assertion helper, which is taken for a mismatch.
     */
    private fun testDataProblemOrNull(testIdentifier: TestIdentifier, throwable: Throwable): TestDataProblem? {
        val [kind, file, comparedFile] = when {
            throwable is TestDataAssertionError -> Triple(throwable.kind, throwable.file, throwable.comparedFile)
            throwable is AssertionFailedError && throwable.expected?.value is FileInfo -> {
                val file = Path((throwable.expected.value as FileInfo).path)
                Triple(TestDataAssertionError.Kind.MISMATCH, file, file)
            }

            else -> return null
        }

        return TestDataProblem(
            testName = testIdentifier.formatTestName(),
            testDataPath = testIdentifier.testDataPath,
            variantChain = testIdentifier.knownVariantChain,
            kind = kind,
            file = file,
            comparedFile = comparedFile,
            actual = throwable.actual?.value as? String,
        )
    }

    /**
     * The frames of [throwable] down to the first one in the test class, or the first [MAX_STACK_FRAMES] if none is.
     */
    private fun TestIdentifier.topStackFrames(throwable: Throwable): List<String> {
        val testClassName = (source.orElse(null) as? MethodSource)?.className
        val frames = throwable.stackTrace.take(MAX_STACK_FRAMES)
        val testFrameIndex = frames.indexOfFirst { it.className == testClassName }
        return (if (testFrameIndex >= 0) frames.take(testFrameIndex + 1) else frames).map(StackTraceElement::toString)
    }

    /**
     * Returns the unique IDs of the tests that finished in this pass without aborting: an aborted test didn't
     * complete, so it keeps its results from an earlier pass.
     */
    fun getExecutedTests(): Set<String> = executedTests - abortedTests

    /**
     * Returns the unique IDs of the tests that failed in this pass, or didn't run because their container failed.
     */
    fun getTestsWithErrors(): Set<String> = testsWithErrors.toSet()

    /**
     * Returns the map of test data paths to sets of variant chains that had mismatches.
     */
    fun getMismatchesByPath(): Map<TestMetadataPath, Set<TestVariantChain>> = mismatchesByPath.mapValues { it.value.toSet() }

    /**
     * Returns the test data files that didn't match, by test unique ID.
     */
    fun getTestDataProblems(): Map<String, List<TestDataProblem>> = testDataProblems.toMap()

    /**
     * Returns the other test failures, by test unique ID, or by container unique ID for the failures of a container.
     */
    fun getTestErrors(): Map<String, List<TestError>> = testErrors.toMap()

    private companion object {
        const val MAX_STACK_FRAMES = 30
    }
}