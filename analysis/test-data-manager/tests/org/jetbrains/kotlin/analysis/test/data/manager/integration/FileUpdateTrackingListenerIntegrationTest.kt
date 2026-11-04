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
            listener.getTestErrors().forEach { add("error: ${it.testName} - ${it.exception}") }
            listener.getTestsWithErrors().forEach { _ -> add("failed test") }
        }.joinToString("\n")
            // The message holds the absolute temporary path, which the listener may truncate
            .replace(Regex("File should not exist: .*"), "File should not exist: <path>")

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
                failed test
            """,
        )
    }

    @Test
    fun `redundant variant file is an error`() {
        setupFiles("test.txt" to "same", "test.js.txt" to "same")
        assertClassified(
            variantChain = listOf("js"),
            actual = "same",
            expected = """
                error: FakeGoldenAnalysisApiTestGenerated.testSymbols - AssertionFailedError: "test.js.txt" had the same content as "test.txt". Delete the prefixed file.
                failed test
            """,
        )
    }

    @Test
    fun `unexpected file is an error`() {
        setupFiles("test.txt" to "unexpected")
        assertClassified(
            variantChain = emptyList(),
            actual = null,
            expected = """
                error: FakeGoldenAnalysisApiTestGenerated.testSymbols - AssertionFailedError: File should not exist: <path>
                failed test
            """,
        )
    }

    @Test
    fun `several failures of one test are an error`() {
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
                error: FakeGoldenAnalysisApiTestGenerated.testSymbols - MultipleFailuresError: Multiple Failures (2 failures)
                	org.opentest4j.AssertionFailedError: Actual data differs from file c...
                failed test
            """,
        )
    }

    @Test
    fun `exception is an error`() {
        assertClassified(
            failure = IllegalStateException("Broken"),
            expected = """
                error: FakeGoldenAnalysisApiTestGenerated.testSymbols - IllegalStateException: Broken
                failed test
            """,
        )
    }
}
