/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTestAssertions
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis.FakeGoldenAnalysisApiTestGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.listeners.FileUpdateTrackingListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.opentest4j.MultipleFailuresError
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Checks how [FileUpdateTrackingListener] classifies the failures [ManagedTestAssertions] produces in `checkTestData`.
 */
internal class FileUpdateTrackingListenerIntegrationTest : AbstractFakeTestIntegrationTest() {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        TestDataManagerMode.isUnderTeamCityOverride = false
        TestDataManagerMode.isExplicitCheckOverride = true
    }

    @AfterEach
    fun tearDown() {
        TestDataManagerMode.isUnderTeamCityOverride = null
        TestDataManagerMode.isExplicitCheckOverride = null
    }

    private val testSymbols: TestIdentifier by lazy {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectMethod(FakeGoldenAnalysisApiTestGenerated::class.java, "testSymbols"))
            .build()

        val testPlan = discoverFakeTestPlan(request)
        testPlan.roots.flatMap(testPlan::getDescendants).single(TestIdentifier::isTest)
    }

    private fun setupFiles(vararg files: Pair<String, String>) {
        for ([name, content] in files) {
            tempDir.resolve(name).writeText("$content\n")
        }
    }

    /** Reports the failure of a CHECK assertion of [actual] against `test.kt` as the result of the fake `testSymbols`. */
    private fun assertClassified(variantChain: TestVariantChain, actual: String?, expected: String) {
        val failure = runCatching {
            ManagedTestAssertions.assertEqualsToTestDataFile(
                testDataPath = tempDir.resolve("test.kt"),
                actual = actual,
                variantChain = variantChain,
                extension = ".txt",
                mode = TestDataManagerMode.CHECK,
            )
        }.exceptionOrNull() ?: error("The assertion passed")

        assertClassified(failure, expected)
    }

    private fun assertClassified(failure: Throwable, expected: String) {
        val listener = FileUpdateTrackingListener()
        listener.executionFinished(testSymbols, TestExecutionResult.failed(failure))

        val actual = buildList {
            listener.getMismatchesByPath().forEach { [path, chains] -> add("mismatch: $path $chains") }
            listener.getTestDataProblems().values.flatten().sortedBy { it.file }.forEach { problem ->
                add("problem: ${problem.testName} ${problem.kind} ${tempDir.relativize(problem.file)}")
                problem.comparedFile?.let { add("  compared with: ${tempDir.relativize(it)}") }
                problem.actual?.let { add("  actual: ${it.trim()}") }
            }

            listener.getTestErrors().values.flatten().forEach { add("error: ${it.testName} - ${it.exceptionClass}: ${it.message}") }
            listener.getTestsWithErrors().forEach { _ -> add("failed test") }
        }.joinToString("\n")

        assertEquals(expected.trimIndent(), actual)
    }

    @Test
    fun `content mismatch is a mismatch`() {
        setupFiles("test.txt" to "expected")
        assertClassified(
            variantChain = emptyList(),
            actual = "actual",
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols MISMATCH test.txt
                  compared with: test.txt
                  actual: actual
                failed test
            """,
        )
    }

    @Test
    fun `variant mismatch is compared with the less specific file`() {
        setupFiles("test.txt" to "golden")
        assertClassified(
            variantChain = listOf("js"),
            actual = "actual",
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols MISMATCH test.js.txt
                  compared with: test.txt
                  actual: actual
                failed test
            """,
        )
    }

    @Test
    fun `missing golden file is a mismatch`() {
        assertClassified(
            variantChain = emptyList(),
            actual = "actual",
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols MISSING_FILE test.txt
                  actual: actual
                failed test
            """,
        )
    }

    @Test
    fun `redundant variant file is a mismatch`() {
        setupFiles("test.txt" to "same", "test.js.txt" to "same")
        assertClassified(
            variantChain = listOf("js"),
            actual = "same",
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols REDUNDANT_FILE test.js.txt
                  compared with: test.txt
                failed test
            """,
        )
    }

    @Test
    fun `unexpected file is a mismatch`() {
        setupFiles("test.txt" to "unexpected")
        assertClassified(
            variantChain = emptyList(),
            actual = null,
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols UNEXPECTED_FILE test.txt
                failed test
            """,
        )
    }

    @Test
    fun `several failures of one test are classified one by one`() {
        setupFiles("test.txt" to "expected", "other.txt" to "expected")
        val failures = listOf("test.kt", "other.kt").map { fileName ->
            runCatching {
                ManagedTestAssertions.assertEqualsToTestDataFile(
                    testDataPath = tempDir.resolve(fileName),
                    actual = "actual",
                    variantChain = emptyList(),
                    extension = ".txt",
                    mode = TestDataManagerMode.CHECK,
                )
            }.exceptionOrNull()!!
        }

        // What `JUnit5Assertions.failAll` throws for several failures
        assertClassified(
            failure = MultipleFailuresError("Multiple Failures", failures),
            expected = """
                mismatch: testData/analysis/api/symbols.kt [[]]
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols MISMATCH other.txt
                  compared with: other.txt
                  actual: actual
                problem: FakeGoldenAnalysisApiTestGenerated.testSymbols MISMATCH test.txt
                  compared with: test.txt
                  actual: actual
                failed test
            """,
        )
    }

    /**
     * Reports the tests of the outer fake class as passed, then [nestedResult] for its nested class, then a teardown
     * failure of the outer class, and renders the errors with their test data paths.
     */
    private fun runOuterTeardownFailureAfterNested(nestedResult: TestExecutionResult): String {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(FakeGoldenAnalysisApiTestGenerated::class.java))
            .build()

        val testPlan = discoverFakeTestPlan(request)
        val identifiers = testPlan.roots.flatMap(testPlan::getDescendants)
        fun container(javaClass: Class<*>) = identifiers.single { (it.source.orElse(null) as? ClassSource)?.javaClass == javaClass }
        val outer = container(FakeGoldenAnalysisApiTestGenerated::class.java)
        val nested = container(FakeGoldenAnalysisApiTestGenerated.Expressions::class.java)

        val listener = FileUpdateTrackingListener()
        listener.testPlanExecutionStarted(testPlan)
        for (test in testPlan.getChildren(outer).filter(TestIdentifier::isTest)) {
            listener.executionFinished(test, TestExecutionResult.successful())
        }

        listener.executionFinished(nested, nestedResult)
        listener.executionFinished(outer, TestExecutionResult.failed(IllegalStateException("Teardown failure")))

        return buildList {
            listener.getTestErrors().values.flatten()
                .map { error -> "error: ${error.testName} - ${error.message} (${error.testDataPath})" }
                .sorted()
                .forEach(::add)

            add("failed tests: ${listener.getTestsWithErrors().size}")
        }.joinToString("\n")
    }

    @Test
    fun `failures of a nested class setup and of the outer class teardown`() {
        assertEquals(
            $$"""
                error: FakeGoldenAnalysisApiTestGenerated - Teardown failure (null)
                error: FakeGoldenAnalysisApiTestGenerated$Expressions.testCall - Setup failure (testData/analysis/api/expressions/call.kt)
                error: FakeGoldenAnalysisApiTestGenerated$Expressions.testLambda - Setup failure (testData/analysis/api/expressions/lambda.kt)
                failed tests: 2
            """.trimIndent(),
            runOuterTeardownFailureAfterNested(TestExecutionResult.failed(IllegalStateException("Setup failure"))),
        )
    }

    @Test
    fun `aborted nested class setup and failure of the outer class teardown`() {
        assertEquals(
            """
                error: FakeGoldenAnalysisApiTestGenerated - Teardown failure (null)
                failed tests: 0
            """.trimIndent(),
            runOuterTeardownFailureAfterNested(TestExecutionResult.aborted(IllegalStateException("Setup abort"))),
        )
    }

    @Test
    fun `exception is an error`() {
        assertClassified(
            failure = IllegalStateException("Broken"),
            expected = """
                error: FakeGoldenAnalysisApiTestGenerated.testSymbols - java.lang.IllegalStateException: Broken
                failed test
            """,
        )
    }
}
