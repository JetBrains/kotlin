/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.listeners

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataAssertionError
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataProblem
import org.jetbrains.kotlin.analysis.test.data.manager.TestError
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.filters.formatTestName
import org.jetbrains.kotlin.analysis.test.data.manager.filters.testDataPath
import org.jetbrains.kotlin.analysis.test.data.manager.filters.variantChain
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
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
 */
internal class FileUpdateTrackingListener : TestExecutionListener {
    /**
     * Unique IDs of all tests that finished, successfully or not.
     */
    private val executedTests = ConcurrentHashMap.newKeySet<String>()

    /**
     * Test unique IDs that failed with real errors (not file updates).
     */
    private val testsWithErrors = ConcurrentHashMap.newKeySet<String>()

    /**
     * Map: testDataPath -> set of prefix lists that failed for this path.
     * Used for CHECK mode summary.
     */
    private val mismatchesByPath = ConcurrentHashMap<String, MutableSet<TestVariantChain>>()

    /**
     * Test data files that didn't match, by test unique ID.
     */
    private val testDataProblems = ConcurrentHashMap<String, List<TestDataProblem>>()

    /**
     * Other test failures, by test unique ID.
     */
    private val testErrors = ConcurrentHashMap<String, List<TestError>>()

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (!testIdentifier.isTest) return
        executedTests.add(testIdentifier.uniqueId)

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
                            .add(testIdentifier.variantChain)
                    }
                } else {
                    // Real error
                    val error = TestError(
                        testName = testIdentifier.formatTestName(),
                        testDataPath = testIdentifier.testDataPath,
                        variantChain = testIdentifier.variantChain,
                        exceptionClass = failure.javaClass.name,
                        message = failure.message,
                        stackTrace = testIdentifier.topStackFrames(failure),
                    )
                    testErrors.merge(testIdentifier.uniqueId, listOf(error), List<TestError>::plus)
                }
            }
        }
    }

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
            variantChain = testIdentifier.variantChain,
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
     * Returns the unique IDs of all tests that finished in this pass.
     */
    fun getExecutedTests(): Set<String> = executedTests.toSet()

    /**
     * Returns the set of test unique IDs that failed with real errors.
     */
    fun getTestsWithErrors(): Set<String> = testsWithErrors.toSet()

    /**
     * Returns the map of test data paths to sets of variant chains that had mismatches.
     */
    fun getMismatchesByPath(): Map<String, Set<TestVariantChain>> = mismatchesByPath.mapValues { it.value.toSet() }

    /**
     * Returns the test data files that didn't match, by test unique ID.
     */
    fun getTestDataProblems(): Map<String, List<TestDataProblem>> = testDataProblems.toMap()

    /**
     * Returns the other test failures, by test unique ID.
     */
    fun getTestErrors(): Map<String, List<TestError>> = testErrors.toMap()

    private companion object {
        const val MAX_STACK_FRAMES = 30
    }
}