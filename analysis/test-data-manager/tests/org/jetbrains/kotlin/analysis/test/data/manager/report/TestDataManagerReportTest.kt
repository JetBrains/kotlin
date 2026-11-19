/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.report

import org.jetbrains.kotlin.analysis.test.data.manager.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.*

class TestDataManagerReportTest {
    @TempDir
    lateinit var tempDir: Path

    private val rootDir: Path get() = tempDir.resolve("repo")

    private fun report(runId: String = "run"): TestDataManagerReport = TestDataManagerReport(
        runId = runId,
        project = ":module",
        mode = TestDataManagerMode.CHECK,
        options = TestDataManagerReport.Options(
            testDataPaths = listOf("module/testData"),
            testClassPattern = null,
            goldenOnly = false,
            incremental = false,
        ),
    )

    private fun problem(kind: TestDataAssertionError.Kind, file: String, comparedFile: String?, actual: String?) = TestDataProblem(
        testName = "FooTestGenerated.testFoo",
        testDataPath = "module/testData/foo.kt",
        variantChain = listOf("knm", "js"),
        kind = kind,
        file = rootDir.resolve(file),
        comparedFile = comparedFile?.let(rootDir::resolve),
        actual = actual,
    )

    @Test
    fun `passed report`() {
        val report = report().apply {
            discoveredTests = 2
            passedTests = 2
        }

        assertEquals(
            """
                {
                  "schemaVersion": 1,
                  "runId": "run",
                  "project": ":module",
                  "mode": "CHECK",
                  "status": "PASSED",
                  "options": {
                    "testDataPath": [
                      "module/testData"
                    ],
                    "testClassPattern": null,
                    "goldenOnly": false,
                    "incremental": false
                  },
                  "runErrors": [],
                  "tests": {
                    "discovered": 2,
                    "passed": 2,
                    "failed": 0,
                    "skipped": 0
                  },
                  "problems": [],
                  "testErrors": [],
                  "fileActions": [],
                  "recoveredTests": []
                }
            """.trimIndent() + "\n",
            report.toJson(rootDir),
        )
    }

    @Test
    fun `failed report lists problems, errors and file actions`() {
        val actualFile = tempDir.resolve("actual/foo.js.txt").toAbsolutePath()
        val report = report().apply {
            discoveredTests = 3
            passedTests = 1
            failedTests = 2
            problems += problem(TestDataAssertionError.Kind.MISMATCH, "module/testData/foo.js.txt", "module/testData/foo.txt", "actual")
            testErrors += TestError(
                testName = "FooTestGenerated.testBar",
                testDataPath = null,
                variantChain = emptyList(),
                exceptionClass = "java.lang.IllegalStateException",
                message = "Broken \"quoted\"\nsecond line",
                stackTrace = listOf("FooTestGenerated.testBar(FooTestGenerated.java:10)"),
            )

            fileActions += FileAction(
                testDataPath = rootDir.resolve("module/testData/foo.kt"),
                file = rootDir.resolve("module/testData/foo.txt"),
                kind = FileAction.Kind.DELETED,
            )

            recoveredTests += RecoveredTest(
                testName = "FooTestGenerated.testFlaky",
                passedInPass = 2,
                problems = emptyList(),
                errors = listOf(testErrors.single().copy(testName = "FooTestGenerated.testFlaky", message = "Flaky")),
            )
        }

        assertEquals(
            """
                  "status": "FAILED",
                  "problems": [
                    {
                      "kind": "MISMATCH",
                      "file": "module/testData/foo.js.txt",
                      "comparedFile": "module/testData/foo.txt",
                      "actualFile": "${actualFile.invariantSeparatorsPathString}",
                      "test": "FooTestGenerated.testFoo",
                      "testDataPath": "module/testData/foo.kt",
                      "variantChain": [
                        "knm",
                        "js"
                      ]
                    }
                  ],
                  "testErrors": [
                    {
                      "test": "FooTestGenerated.testBar",
                      "testDataPath": null,
                      "variantChain": [],
                      "exception": "java.lang.IllegalStateException",
                      "message": "Broken \"quoted\"\nsecond line",
                      "stackTrace": [
                        "FooTestGenerated.testBar(FooTestGenerated.java:10)"
                      ]
                    }
                  ],
                  "fileActions": [
                    {
                      "kind": "DELETED",
                      "file": "module/testData/foo.txt",
                      "testDataPath": "module/testData/foo.kt"
                    }
                  ],
                  "recoveredTests": [
                    {
                      "test": "FooTestGenerated.testFlaky",
                      "passedInPass": 2,
                      "failures": [
                        "java.lang.IllegalStateException: Flaky"
                      ]
                    }
                  ]
            """.trimIndent(),
            report.toJson(rootDir, report.problems.associateWith { actualFile })
                .sections("status", "problems", "testErrors", "fileActions", "recoveredTests"),
        )
    }

    @Test
    fun `run errors make the status ERROR`() {
        val report = report().apply { runErrors += "testDataPath 'x' does not exist" }

        assertEquals(
            """
                  "status": "ERROR",
                  "runErrors": [
                    "testDataPath 'x' does not exist"
                  ],
            """.trimIndent(),
            report.toJson(rootDir).sections("status", "runErrors"),
        )
    }

    @Test
    fun `write stores the report and the actual output under the run id`() {
        val reportDirectory = tempDir.resolve("build/test-data-manager")
        val report = report(runId = "0193").apply {
            problems += problem(TestDataAssertionError.Kind.MISSING_FILE, "module/testData/foo.txt", comparedFile = null, actual = "new\n")
            problems += problem(
                kind = TestDataAssertionError.Kind.REDUNDANT_FILE,
                file = "module/testData/foo.js.txt",
                comparedFile = "module/testData/foo.txt",
                actual = null,
            )
        }

        val reportFile = report.write(reportDirectory, rootDir)

        val actualFile = reportDirectory.resolve("0193/actual/module/testData/foo.txt")
        assertEquals(reportDirectory.resolve("0193.json"), reportFile)
        assertEquals("new\n", actualFile.readText())
        assertEquals(
            """
                "actualFile": null,
                "actualFile": "${actualFile.toAbsolutePath().invariantSeparatorsPathString}",
            """.trimIndent(),
            reportFile.readText().lines().filter { "\"actualFile\"" in it }.joinToString("\n") { it.trim() },
        )
    }

    @Test
    fun `write keeps the actual output of each problem on the same file`() {
        val reportDirectory = tempDir.resolve("build/test-data-manager")
        val report = report(runId = "shared").apply {
            problems += problem(TestDataAssertionError.Kind.MISMATCH, "module/testData/foo.js.txt", "module/testData/foo.txt", "js")
                .copy(testName = "FooJsTestGenerated.testFoo")
            problems += problem(TestDataAssertionError.Kind.MISMATCH, "module/testData/foo.js.txt", "module/testData/foo.txt", "knm js")
                .copy(testName = "FooKnmJsTestGenerated.testFoo")
        }

        val reportFile = report.write(reportDirectory, rootDir)

        val actualDirectory = reportDirectory.resolve("shared/actual")
        assertEquals(
            """
                "actualFile": "${actualDirectory.resolve("module/testData/foo.js.txt").toAbsolutePath().invariantSeparatorsPathString}",
                "test": "FooJsTestGenerated.testFoo",
                "actualFile": "${actualDirectory.resolve("2/module/testData/foo.js.txt").toAbsolutePath().invariantSeparatorsPathString}",
                "test": "FooKnmJsTestGenerated.testFoo",
            """.trimIndent(),
            reportFile.readText().lines().filter { "\"actualFile\"" in it || "\"test\"" in it }.joinToString("\n") { it.trim() },
        )
        assertEquals("js", actualDirectory.resolve("module/testData/foo.js.txt").readText())
        assertEquals("knm js", actualDirectory.resolve("2/module/testData/foo.js.txt").readText())
    }

    @Test
    fun `write keeps the most recent runs`() {
        val reportDirectory = tempDir.resolve("build/test-data-manager").createDirectories()
        for (index in 1..3) {
            val oldReport = reportDirectory.resolve("old$index.json")
            oldReport.writeText("{}")
            oldReport.setLastModifiedTime(FileTime.fromMillis(index * 1000L))
            reportDirectory.resolve("old$index/actual").createDirectories()
        }

        report(runId = "new").write(reportDirectory, rootDir, keptRuns = 2)

        assertEquals(
            listOf("new.json", "old3", "old3.json"),
            reportDirectory.listDirectoryEntries().map { it.name }.sorted(),
        )
    }

    @Test
    fun `write keeps the report of the kept run`() {
        val reportDirectory = tempDir.resolve("build/test-data-manager").createDirectories()
        for (index in 1..3) {
            val oldReport = reportDirectory.resolve("old$index.json")
            oldReport.writeText("{}")
            oldReport.setLastModifiedTime(FileTime.fromMillis(index * 1000L))
            reportDirectory.resolve("old$index/actual").createDirectories()
        }

        report(runId = "new").write(reportDirectory, rootDir, keptRuns = 2, keptRunId = "old1")

        assertEquals(
            listOf("new.json", "old1", "old1.json", "old3", "old3.json"),
            reportDirectory.listDirectoryEntries().map { it.name }.sorted(),
        )
    }

    @Test
    fun `actual output of a file outside the root never goes to the file itself`() {
        val reportDirectory = tempDir.resolve("build/test-data-manager")
        val outsideFile = tempDir.resolve("elsewhere/foo.txt").createParentDirectoriesAndWrite("expected\n")
        val report = report(runId = "outside").apply {
            problems += TestDataProblem(
                testName = "FooTestGenerated.testFoo",
                testDataPath = null,
                variantChain = emptyList(),
                kind = TestDataAssertionError.Kind.MISMATCH,
                file = outsideFile,
                comparedFile = outsideFile,
                actual = "actual\n",
            )
        }

        report.write(reportDirectory, rootDir)

        assertEquals("expected\n", outsideFile.readText())
        val actualFiles = reportDirectory.resolve("outside/actual/outside").listDirectoryEntries().single().listDirectoryEntries()
        assertEquals(listOf("foo.txt: actual"), actualFiles.map { "${it.name}: ${it.readText().trim()}" })
    }

    @Test
    fun `run ids are file names`() {
        assertEquals(
            """
                0193-ab_c.1: ok
                ..: runId '..' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'
                .: runId '.' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'
                a/b: runId 'a/b' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'
            """.trimIndent(),
            listOf("0193-ab_c.1", "..", ".", "a/b").joinToString("\n") { "$it: ${TestDataManagerReport.runIdError(it) ?: "ok"}" },
        )
    }

    private fun Path.createParentDirectoriesAndWrite(text: String): Path = apply {
        parent.createDirectories()
        writeText(text)
    }

    /** The top-level entries of the rendered report with the given [keys], in the report's order. */
    private fun String.sections(vararg keys: String): String {
        val lines = lines()
        return lines.indices
            .filter { index -> keys.any { lines[index].startsWith("  \"$it\": ") } }
            .flatMap { start ->
                val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("  \"") || lines[it] == "}" } ?: lines.size
                lines.subList(start, end)
            }
            .joinToString("\n")
            .trimIndent()
    }
}
