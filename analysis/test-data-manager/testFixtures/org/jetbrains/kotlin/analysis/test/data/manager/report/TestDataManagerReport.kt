/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.report

import org.jetbrains.kotlin.analysis.test.data.manager.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * The machine-readable result of one test data manager run in one module, written as `<runId>.json`
 * into the module's report directory by [write].
 *
 * The [runId] identifies the run: a caller running several modules passes the same one to all of them and finds
 * every module's report by it. A missing report means the module didn't run or its JVM crashed.
 *
 * File actions cover only the files written through `ManagedTestAssertions`.
 */
internal class TestDataManagerReport(
    val runId: String,
    val project: String?,
    val mode: TestDataManagerMode?,
    val options: Options,
) {
    class Options(
        val testDataPaths: List<String>?,
        val testClassPattern: String?,
        val goldenOnly: Boolean,
        val incremental: Boolean,
    )

    /** Errors that stopped the run itself: invalid options, variant chain conflicts, crashes of the runner. */
    val runErrors: MutableList<String> = mutableListOf()

    var discoveredTests: Int = 0
    var passedTests: Int = 0
    var failedTests: Int = 0
    var skippedTests: Int = 0

    val problems: MutableList<TestDataProblem> = mutableListOf()
    val testErrors: MutableList<TestError> = mutableListOf()
    val fileActions: MutableList<FileAction> = mutableListOf()

    /** Tests that passed only on a re-run; they don't fail the run. */
    val recoveredTests: MutableList<RecoveredTest> = mutableListOf()

    val status: RunStatus
        get() = when {
            runErrors.isNotEmpty() -> RunStatus.ERROR
            problems.isNotEmpty() || testErrors.isNotEmpty() -> RunStatus.FAILED
            else -> RunStatus.PASSED
        }

    /**
     * Writes the report into [reportDirectory] as `<runId>.json`, and the actual output of each problem that has one
     * under `<runId>/actual/`, at the path of its file relative to [rootDir], so it can be diffed with the expected file.
     * A file outside [rootDir] goes to `<runId>/actual/outside/<hash of its directory>/<name>`. The actual outputs of further
     * problems on the same file go to `<runId>/actual/<n>/`.
     *
     * Keeps the [keptRuns] most recent reports of the directory, and the one of [keptRunId], and deletes the older ones.
     *
     * @return the written report file
     */
    fun write(reportDirectory: Path, rootDir: Path?, keptRuns: Int = DEFAULT_KEPT_RUNS, keptRunId: String? = null): Path {
        reportDirectory.createDirectories()
        val runDirectory = reportDirectory.resolve(runId)
        if (runDirectory.exists()) {
            @OptIn(ExperimentalPathApi::class)
            runDirectory.deleteRecursively()
        }

        // Tests of different variant chains can fail on the same file: the second one goes to `actual/2/...`, and so on
        val usedPaths = mutableMapOf<String, Int>()
        val actualFiles = problems.sortedWith(compareBy({ it.file }, { it.testName })).associateWith { problem ->
            val actual = problem.actual ?: return@associateWith null
            val path = actualFilePath(problem.file, rootDir)
            val occurrence = usedPaths.merge(path, 1, Int::plus)!!
            val directory = runDirectory.resolve("actual").let { if (occurrence == 1) it else it.resolve(occurrence.toString()) }
            directory.resolve(path).also { file ->
                file.createParentDirectories()
                file.writeText(actual)
            }
        }

        val reportFile = reportDirectory.resolve("$runId.json")
        reportFile.writeText(toJson(rootDir, actualFiles))
        deleteOldRuns(reportDirectory, keptRuns, keptRunId)
        return reportFile
    }

    /**
     * Renders the report as JSON. Paths inside [rootDir] are written relative to it, the others as they are.
     *
     * @param actualFiles the file [write] saved the actual output of each problem to, written as the problem's
     *                    `actualFile` with an absolute path even inside [rootDir]; a problem without an entry, or with
     *                    a `null` one because it has no actual output, gets a `null` `actualFile`
     */
    internal fun toJson(rootDir: Path?, actualFiles: Map<TestDataProblem, Path?> = emptyMap()): String {
        val report = ModuleReport(
            schemaVersion = ModuleReport.SCHEMA_VERSION,
            runId = runId,
            project = project,
            mode = mode,
            status = status,
            options = ModuleReport.Options(
                testDataPaths = options.testDataPaths,
                testClassPattern = options.testClassPattern,
                goldenOnly = options.goldenOnly,
                incremental = options.incremental,
            ),
            runErrors = runErrors,
            tests = TestCounts(discoveredTests, passedTests, failedTests, skippedTests),
            problems = problems.sortedWith(compareBy({ it.file }, { it.testName })).map { problem ->
                ModuleReport.Problem(
                    kind = problem.kind,
                    file = problem.file.displayPath(rootDir),
                    comparedFile = problem.comparedFile?.displayPath(rootDir),
                    actualFile = actualFiles[problem]?.toAbsolutePath()?.invariantSeparatorsPathString,
                    test = problem.testName,
                    testDataPath = problem.testDataPath?.path,
                    variantChain = problem.variantChain,
                )
            },
            testErrors = testErrors.sortedBy { it.testName }.map { error ->
                ModuleReport.TestError(
                    test = error.testName,
                    testDataPath = error.testDataPath?.path,
                    variantChain = error.variantChain,
                    exception = error.exceptionClass,
                    message = error.message?.let { if (it.length > MAX_MESSAGE_LENGTH) it.take(MAX_MESSAGE_LENGTH) + "..." else it },
                    stackTrace = error.stackTrace,
                )
            },
            fileActions = fileActions.sortedBy { it.file }.map { action ->
                ModuleReport.FileAction(
                    kind = action.kind,
                    file = action.file.displayPath(rootDir),
                    testDataPath = action.testDataPath.displayPath(rootDir),
                )
            },
            recoveredTests = recoveredTests.sortedBy { it.testName }.map { test ->
                ModuleReport.RecoveredTest(
                    test = test.testName,
                    passedInPass = test.passedInPass,
                    failures = test.errors.map { "${it.exceptionClass}: ${it.message.orEmpty()}".trimEnd(' ', ':') } +
                            test.problems.map { "${it.kind} ${it.file.displayPath(rootDir)}" },
                )
            },
        )

        return TEST_DATA_MANAGER_JSON.encodeToString(ModuleReport.serializer(), report) + "\n"
    }

    companion object {
        const val DEFAULT_KEPT_RUNS = 5
        private const val MAX_MESSAGE_LENGTH = 4000

        /**
         * Keeps the [keptRuns] most recently written reports in [reportDirectory], and the one of [keptRunId], with their
         * run directories.
         */
        private fun deleteOldRuns(reportDirectory: Path, keptRuns: Int, keptRunId: String?) {
            val reports = reportDirectory.listDirectoryEntries("*.json").sortedByDescending { it.getLastModifiedTime() }
            for (report in reports.drop(keptRuns).filter { it.nameWithoutExtension != keptRunId }) {
                val runDirectory = reportDirectory.resolve(report.nameWithoutExtension)
                if (runDirectory.exists()) {
                    @OptIn(ExperimentalPathApi::class)
                    runDirectory.deleteRecursively()
                }

                report.deleteIfExists()
            }
        }
    }
}

/**
 * Where the actual output for [file] goes under the `actual` directory of a run: always a relative path, so resolving it
 * can never point back at [file] itself or anywhere outside that directory.
 */
private fun actualFilePath(file: Path, rootDir: Path?): String {
    val absoluteFile = file.toAbsolutePath().normalize()
    val absoluteRoot = rootDir?.toAbsolutePath()?.normalize()
    if (absoluteRoot != null && absoluteFile.startsWith(absoluteRoot) && absoluteFile != absoluteRoot) {
        return absoluteRoot.relativize(absoluteFile).invariantSeparatorsPathString
    }

    val directoryHash = Integer.toHexString(absoluteFile.parent?.invariantSeparatorsPathString.hashCode())
    return "outside/$directoryHash/${absoluteFile.fileName}"
}

/** The path relative to [rootDir] when it is inside, for shorter output; otherwise the path itself. */
internal fun Path.displayPath(rootDir: Path?): String {
    val path = rootDir?.takeIf(::startsWith)?.relativize(this) ?: this
    return path.invariantSeparatorsPathString
}

