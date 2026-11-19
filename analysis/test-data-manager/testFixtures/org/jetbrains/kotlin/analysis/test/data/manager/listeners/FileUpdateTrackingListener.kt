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
 */
internal class FileUpdateTrackingListener : TestExecutionListener {
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
     * Test data files that didn't match, for the CHECK mode summary.
     */
    private val testDataProblems = ConcurrentHashMap.newKeySet<TestDataProblem>()

    /**
     * Test errors with formatted names for CHECK mode summary.
     */
    private val testErrors = ConcurrentHashMap.newKeySet<TestError>()

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (!testIdentifier.isTest) return

        // Track failures
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) {
            testsWithErrors.add(testIdentifier.uniqueId)

            val throwable = testExecutionResult.throwable.orElse(null) ?: return
            // Several failures of one test, e.g., from `JUnit5Assertions.failAll`, are classified one by one
            val failures = (throwable as? MultipleFailuresError)?.failures?.takeIf { it.isNotEmpty() } ?: listOf(throwable)
            for (failure in failures) {
                val problem = testDataProblemOrNull(testIdentifier, failure)
                if (problem != null) {
                    testDataProblems.add(problem)
                    val testDataPath = testIdentifier.testDataPath
                    if (testDataPath != null) {
                        mismatchesByPath
                            .getOrPut(testDataPath) { ConcurrentHashMap.newKeySet() }
                            .add(testIdentifier.variantChain)
                    }
                } else {
                    // Real error
                    testErrors.add(
                        TestError(
                            testName = testIdentifier.formatTestName(),
                            exception = formatException(failure),
                        )
                    )
                }
            }
        }
    }

    /**
     * The test data problem [throwable] reports: a [TestDataAssertionError], or a comparison with an expected file
     * from another assertion helper, which is taken for a mismatch.
     */
    private fun testDataProblemOrNull(testIdentifier: TestIdentifier, throwable: Throwable): TestDataProblem? {
        val [kind, file] = when {
            throwable is TestDataAssertionError -> throwable.kind to throwable.file
            throwable is AssertionFailedError && throwable.expected?.value is FileInfo ->
                TestDataAssertionError.Kind.MISMATCH to Path((throwable.expected.value as FileInfo).path)

            else -> return null
        }

        return TestDataProblem(testIdentifier.formatTestName(), kind, file)
    }

    private fun formatException(throwable: Throwable): String {
        val name = throwable.javaClass.simpleName
        val message = throwable.message?.let {
            if (it.length > 100) it.substring(0, 100) + "..." else it
        }.orEmpty()

        return if (message.isNotEmpty()) "$name: $message" else name
    }

    /**
     * Returns the set of test unique IDs that failed with real errors.
     */
    fun getTestsWithErrors(): Set<String> = testsWithErrors.toSet()

    /**
     * Returns the map of test data paths to sets of variant chains that had mismatches.
     */
    fun getMismatchesByPath(): Map<String, Set<TestVariantChain>> = mismatchesByPath.mapValues { it.value.toSet() }

    /**
     * Returns the test data files that didn't match.
     */
    fun getTestDataProblems(): Set<TestDataProblem> = testDataProblems.toSet()

    /**
     * Returns the set of test errors with formatted names.
     */
    fun getTestErrors(): Set<TestError> = testErrors.toSet()
}