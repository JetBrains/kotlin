/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.TEST_DATA_MANAGER_OPTIONS_MODE
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeConflictingTestABGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.runner.FakeRunnerScenario
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
            add("tests: ${outcome.totalTests}, passed: ${outcome.passedTests}, failed: ${outcome.failedTests}")
            add("executions: ${outcome.testExecutions}, successful: ${outcome.isSuccessful}")
            outcome.runErrors.forEach { add("run error: ${it.replace(tempDir.toString(), "<root>")}") }
            outcome.mismatchesByPath.toSortedMap().forEach { [path, chains] -> add("mismatch: $path $chains") }
            outcome.errors.map { "error: ${it.testName} - ${it.exception}" }.sorted().forEach(::add)
            testDataDirectory.listDirectoryEntries("*.txt").sortedBy { it.name }.forEach { add("${it.name}: ${it.readText().trim()}") }
        }.joinToString("\n")
    }

    @Test
    fun `passing run`() {
        setupFiles("first.txt" to "first")
        FakeRunnerScenario.outputs["first"] = "first"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0
                executions: 7, successful: true
                first.txt: first
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `check reports mismatches and re-runs the failing tests once`() {
        setupFiles("first.txt" to "expected")
        FakeRunnerScenario.outputs["first"] = "actual"

        assertEquals(
            """
                tests: 7, passed: 4, failed: 3
                executions: 10, successful: false
                mismatch: runner/first.kt [[], [js], [knm, js]]
                first.txt: expected
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `nonexistent test data path fails the run`() {
        assertEquals(
            """
                tests: 0, passed: 0, failed: 0
                executions: 0, successful: false
                run error: testDataPath 'runner/nope' does not exist; paths are relative to the repository root <root>
            """.trimIndent(),
            run(TestDataManagerMode.CHECK, testDataPath = "runner/nope"),
        )
    }

    @Test
    fun `mismatch of a test without TestMetadata passes the run`() {
        setupFiles("noMetadata.txt" to "expected")
        FakeRunnerScenario.outputs["noMetadata"] = "actual"

        assertEquals(
            """
                tests: 7, passed: 6, failed: 1
                executions: 8, successful: true
                noMetadata.txt: expected
            """.trimIndent(),
            run(TestDataManagerMode.CHECK),
        )
    }

    @Test
    fun `failure fixed by a re-run is still reported`() {
        FakeRunnerScenario.flakyFailures.set(1)

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0
                executions: 8, successful: false
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
                tests: 7, passed: 7, failed: 0
                executions: 7, successful: true
                first.txt: new
                second.js.txt: js second
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE),
        )
    }

    @Test
    fun `incremental update runs variants only for changed golden paths and counts skipped tests as passed`() {
        setupFiles("first.txt" to "old", "second.txt" to "second")
        FakeRunnerScenario.outputs["first"] = "new"
        FakeRunnerScenario.outputs["second"] = "second"

        assertEquals(
            """
                tests: 7, passed: 7, failed: 0
                executions: 6, successful: true
                first.txt: new
                second.txt: second
            """.trimIndent(),
            run(TestDataManagerMode.UPDATE, incremental = true),
        )
    }

    @Test
    fun `variant chain conflicts stop the run`() {
        System.setProperty(TEST_DATA_MANAGER_OPTIONS_MODE, "check")
        val config = LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build()
        val exception = assertThrows<IllegalStateException> {
            LauncherFactory.openSession(config).use { session ->
                TestDataManagerRunner.discoverAndRunTests(
                    testClassPattern = null,
                    testDataPath = null,
                    goldenOnly = false,
                    incremental = false,
                    PackageNameFilter.includePackageNames(FakeConflictingTestABGenerated::class.java.`package`.name),
                    launcherSession = session,
                )
            }
        }

        // The order of the conflicts follows the discovery order
        assertEquals(
            listOf(
                "Variant chain conflicts detected:",
                "[a, b] writes 'b' which [b, a] reads",
                "[b, a] writes 'a' which [a, b] reads",
                "[x, b] writes 'b' which [a, b] reads",
                "[x, b] writes 'b' which [b, a] reads",
            ),
            exception.message!!.lines().map(String::trim).filter(String::isNotEmpty).sorted(),
        )
    }
}
