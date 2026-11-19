/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.*
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeConflictingTestABGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.runner.FakeRunnerScenario
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Runs the runner over the suites in the `fakes.runner` package, configured through [FakeRunnerScenario].
 *
 * The suites resolve `runner/<name>.kt` against [tempDir]; the golden one has `first`, `second` and `flaky` tests,
 * the `[js]` and `[knm, js]` ones have `first` (and `second`), and one suite has no `@TestMetadata`.
 */
internal class TestDataManagerRunnerIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private val testDataDirectory: Path get() = tempDir.resolve("runner")

    @BeforeEach
    fun setUp() {
        FakeRunnerScenario.reset(tempDir)
        testDataDirectory.createDirectories()
        for (name in listOf("first", "second", "flaky", "noMetadata")) {
            testDataDirectory.resolve("$name.kt").writeText("// $name")
        }
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty(TEST_DATA_MANAGER_OPTIONS_MODE)
        System.clearProperty(TEST_DATA_MANAGER_OPTIONS_ROOT_DIR)
    }

    private fun setupFiles(vararg files: Pair<String, String>) {
        for ([name, content] in files) {
            testDataDirectory.resolve(name).writeText("$content\n")
        }
    }

    /** Runs the fake suites in [mode] and renders the outcome and the expected files afterwards. */
    private fun run(
        mode: TestDataManagerMode,
        testDataPath: String? = null,
        incremental: Boolean = false,
    ): String {
        System.setProperty(TEST_DATA_MANAGER_OPTIONS_MODE, mode.name.lowercase())
        val config = LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build()
        val outcome = LauncherFactory.openSession(config).use { session ->
            TestDataManagerRunner.discoverAndRunTests(
                testClassPattern = null,
                testDataPath = testDataPath,
                goldenOnly = false,
                incremental = incremental,
                PackageNameFilter.includePackageNames(FakeRunnerScenario::class.java.`package`.name),
                rootDir = tempDir,
                launcherSession = session,
            )
        }

        return buildList {
            add("tests: ${outcome.totalTests}, passed: ${outcome.passedTests}, failed: ${outcome.failedTests}, skipped: ${outcome.skippedTests}")
            add("executions: ${outcome.testExecutions}, successful: ${outcome.isSuccessful}")
            outcome.runErrors.forEach { add("run error: ${it.replace(tempDir.toString(), "<root>")}") }
            outcome.problems.map { "${it.kind} ${tempDir.relativize(it.file)} (${it.testName})" }.sorted().forEach(::add)
            outcome.errors.map { "error: ${it.testName} - ${it.exception}" }.sorted().forEach(::add)
            outcome.recoveredTests.map { test ->
                val failures = test.errors.map { it.exception } + test.problems.map { "${it.kind} ${tempDir.relativize(it.file)}" }
                "recovered in pass ${test.passedInPass}: ${test.testName} - ${failures.joinToString()}"
            }.sorted().forEach(::add)
            outcome.fileActions.map { "${it.kind} ${tempDir.relativize(it.file)}" }.sorted().forEach(::add)
            testDataDirectory.listDirectoryEntries("*.txt").sortedBy { it.name }.forEach { add("${it.name}: ${it.readText().trim()}") }
        }.joinToString("\n")
    }

    @Test
    fun `passing run`() {
        setupFiles("first.txt" to "first")
        FakeRunnerScenario.outputs["first"] = "first"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0, skipped: 0
                executions: 7, successful: true
                first.txt: first
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `check reports mismatches without re-running the failing tests`() {
        setupFiles("first.txt" to "expected")
        FakeRunnerScenario.outputs["first"] = "actual"

        assertEquals(
            """
                tests: 7, passed: 4, failed: 3, skipped: 0
                executions: 7, successful: false
                MISMATCH runner/first.js.txt (FakeRunnerJsTestGenerated.testFirst)
                MISMATCH runner/first.js.txt (FakeRunnerKnmJsTestGenerated.testFirst)
                MISMATCH runner/first.txt (FakeRunnerGoldenTestGenerated.testFirst)
                first.txt: expected
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `nonexistent test data path fails the run`() {
        assertEquals(
            """
                tests: 0, passed: 0, failed: 0, skipped: 0
                executions: 0, successful: false
                run error: testDataPath 'runner/nope' does not exist; paths are relative to the repository root <root>
            """.trimIndent(),
            run(TestDataManagerMode.CHECK, testDataPath = "runner/nope"),
        )
    }

    @Test
    fun `mismatch of a test without TestMetadata fails the run`() {
        setupFiles("noMetadata.txt" to "expected")
        FakeRunnerScenario.outputs["noMetadata"] = "actual"

        assertEquals(
            """
                tests: 7, passed: 6, failed: 1, skipped: 0
                executions: 7, successful: false
                MISMATCH runner/noMetadata.txt (FakeRunnerNoMetadataTestGenerated.testWithoutMetadata)
                noMetadata.txt: expected
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `failure fixed by a re-run is reported as recovered`() {
        FakeRunnerScenario.flakyFailures.set(1)

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0, skipped: 0
                executions: 8, successful: true
                recovered in pass 2: FakeRunnerGoldenTestGenerated.testFlaky - IllegalStateException: Flaky failure
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE),
        )
    }

    @Test
    fun `check does not re-run a failing test`() {
        FakeRunnerScenario.flakyFailures.set(1)

        assertEquals(
            """
                tests: 7, passed: 6, failed: 1, skipped: 0
                executions: 7, successful: false
                error: FakeRunnerGoldenTestGenerated.testFlaky - IllegalStateException: Flaky failure
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `update writes golden and variant files`() {
        setupFiles("first.txt" to "old")
        FakeRunnerScenario.outputs["first"] = "new"
        FakeRunnerScenario.outputs["js:second"] = "js second"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0, skipped: 0
                executions: 7, successful: true
                CREATED runner/second.js.txt
                UPDATED runner/first.txt
                first.txt: new
                second.js.txt: js second
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE),
        )
    }

    @Test
    fun `incremental update runs variants only for changed golden paths`() {
        setupFiles("first.txt" to "old", "second.txt" to "second")
        FakeRunnerScenario.outputs["first"] = "new"
        FakeRunnerScenario.outputs["second"] = "second"

        assertEquals(
            """
                tests: 7, passed: 6, failed: 0, skipped: 1
                executions: 6, successful: true
                UPDATED runner/first.txt
                first.txt: new
                second.txt: second
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE, incremental = true),
        )
    }

    @Test
    fun `file written by two groups is listed once with its net effect`() {
        setupFiles("first.txt" to "golden")
        FakeRunnerScenario.outputs["first"] = "golden"
        FakeRunnerScenario.outputs["js:first"] = "js"
        FakeRunnerScenario.outputs["knmJs:first"] = "knm js"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0, skipped: 0
                executions: 7, successful: true
                CREATED runner/first.js.txt
                first.js.txt: knm js
                first.txt: golden
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE),
        )
    }

    @Test
    fun `file created by one group and deleted by another is not listed`() {
        setupFiles("first.txt" to "golden")
        FakeRunnerScenario.outputs["first"] = "golden"
        FakeRunnerScenario.outputs["js:first"] = "js"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0, skipped: 0
                executions: 7, successful: true
                first.txt: golden
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE),
        )
    }

    @Test
    fun `compact summary lists the first 50 changed files`() {
        System.setProperty(TEST_DATA_MANAGER_OPTIONS_ROOT_DIR, tempDir.toString())
        val fileActions = (0 until 120).map { index ->
            FileAction(testDataDirectory.resolve("first.kt"), testDataDirectory.resolve("%03d.txt".format(index)), FileAction.Kind.UPDATED)
        }

        val output = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(output, true))
        try {
            TestDataManagerRunner.printSummary(emptySet(), emptySet(), fileActions, totalTests = 120, passedTests = 120, skippedTests = 0)
        } finally {
            System.setOut(originalOut)
        }

        val expected = buildList {
            add("120 tests completed, 120 passed, 0 failed")
            (0 until 50).mapTo(this) { "UPDATED runner/%03d.txt".format(it) }
            add("... and 70 more, see the report")
        }
        assertEquals(expected.joinToString("\n"), output.toString().trim().lines().joinToString("\n"))
    }

    @Test
    fun `variant chain conflicts fail the run`() {
        System.setProperty(TEST_DATA_MANAGER_OPTIONS_MODE, "check")
        val config = LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build()
        val outcome = LauncherFactory.openSession(config).use { session ->
            TestDataManagerRunner.discoverAndRunTests(
                testClassPattern = null,
                testDataPath = null,
                goldenOnly = false,
                incremental = false,
                PackageNameFilter.includePackageNames(FakeConflictingTestABGenerated::class.java.`package`.name),
                launcherSession = session,
            )
        }

        // The order of the conflicts follows the discovery order
        assertEquals(
            listOf(
                "Variant chain conflict: [a, b] writes 'b' which [b, a] reads",
                "Variant chain conflict: [b, a] writes 'a' which [a, b] reads",
                "Variant chain conflict: [x, b] writes 'b' which [a, b] reads",
                "Variant chain conflict: [x, b] writes 'b' which [b, a] reads",
                "successful: false, executions: 0",
            ),
            outcome.runErrors.sorted() + "successful: ${outcome.isSuccessful}, executions: ${outcome.testExecutions}",
        )
    }
}
