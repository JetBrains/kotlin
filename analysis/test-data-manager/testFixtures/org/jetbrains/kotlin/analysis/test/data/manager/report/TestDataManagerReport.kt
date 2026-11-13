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

    enum class Status {
        /** All tests passed. */
        PASSED,

        /** Some tests have [problems] or [testErrors]. */
        FAILED,

        /** The run couldn't start or finish, see [runErrors]. */
        ERROR,
    }

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

    val status: Status
        get() = when {
            runErrors.isNotEmpty() -> Status.ERROR
            problems.isNotEmpty() || testErrors.isNotEmpty() -> Status.FAILED
            else -> Status.PASSED
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
     * Renders the report; paths inside [rootDir] are relative to it, [actualFiles] are absolute.
     */
    internal fun toJson(rootDir: Path?, actualFiles: Map<TestDataProblem, Path?> = emptyMap()): String {
        val json = linkedMapOf(
            "schemaVersion" to SCHEMA_VERSION,
            "runId" to runId,
            "project" to project,
            "mode" to mode?.name,
            "status" to status.name,
            "options" to linkedMapOf(
                "testDataPath" to options.testDataPaths,
                "testClassPattern" to options.testClassPattern,
                "goldenOnly" to options.goldenOnly,
                "incremental" to options.incremental,
            ),
            "runErrors" to runErrors,
            "tests" to linkedMapOf(
                "discovered" to discoveredTests,
                "passed" to passedTests,
                "failed" to failedTests,
                "skipped" to skippedTests,
            ),
            "problems" to problems.sortedWith(compareBy({ it.file }, { it.testName })).map { problem ->
                linkedMapOf(
                    "kind" to problem.kind.name,
                    "file" to problem.file.displayPath(rootDir),
                    "comparedFile" to problem.comparedFile?.displayPath(rootDir),
                    "actualFile" to actualFiles[problem]?.toAbsolutePath()?.invariantSeparatorsPathString,
                    "test" to problem.testName,
                    "testDataPath" to problem.testDataPath,
                    "variantChain" to problem.variantChain,
                )
            },
            "testErrors" to testErrors.sortedBy { it.testName }.map { error ->
                linkedMapOf(
                    "test" to error.testName,
                    "testDataPath" to error.testDataPath,
                    "variantChain" to error.variantChain,
                    "exception" to error.exceptionClass,
                    "message" to error.message?.let { if (it.length > MAX_MESSAGE_LENGTH) it.take(MAX_MESSAGE_LENGTH) + "..." else it },
                    "stackTrace" to error.stackTrace,
                )
            },
            "fileActions" to fileActions.sortedBy { it.file }.map { action ->
                linkedMapOf(
                    "kind" to action.kind.name,
                    "file" to action.file.displayPath(rootDir),
                    "testDataPath" to action.testDataPath.displayPath(rootDir),
                )
            },
            "recoveredTests" to recoveredTests.sortedBy { it.testName }.map { test ->
                linkedMapOf(
                    "test" to test.testName,
                    "passedInPass" to test.passedInPass,
                    "failures" to test.errors.map { "${it.exceptionClass}: ${it.message.orEmpty()}".trimEnd(' ', ':') } +
                            test.problems.map { "${it.kind} ${it.file.displayPath(rootDir)}" },
                )
            },
        )

        return buildString { appendJson(json, indent = "") }.plus("\n")
    }

    companion object {
        /**
         * Why [runId] can't name a report, or `null` if it can: it names a file and a directory next to it,
         * so it must not contain path separators or be `.` or `..`.
         */
        fun runIdError(runId: String): String? =
            if (runId.matches(RUN_ID_PATTERN) && runId != "." && runId != "..") null
            else "runId '$runId' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'"

        private val RUN_ID_PATTERN = Regex("[A-Za-z0-9._-]+")

        const val SCHEMA_VERSION = 1
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

private fun StringBuilder.appendJson(value: Any?, indent: String) {
    when (value) {
        null -> append("null")
        is String -> appendJsonString(value)
        is Number, is Boolean -> append(value.toString())
        is Map<*, *> -> appendJsonContainer(value.entries, '{', '}', indent) { [key, entryValue], nestedIndent ->
            appendJsonString(key.toString())
            append(": ")
            appendJson(entryValue, nestedIndent)
        }

        is Collection<*> -> appendJsonContainer(value, '[', ']', indent) { element, nestedIndent ->
            appendJson(element, nestedIndent)
        }

        else -> error("Unsupported JSON value: ${value::class}")
    }
}

private inline fun <T> StringBuilder.appendJsonContainer(
    elements: Collection<T>,
    open: Char,
    close: Char,
    indent: String,
    appendElement: StringBuilder.(T, String) -> Unit,
) {
    if (elements.isEmpty()) {
        append(open).append(close)
        return
    }

    val nestedIndent = "$indent  "
    append(open).append('\n')
    for ([index, element] in elements.withIndex()) {
        append(nestedIndent)
        appendElement(element, nestedIndent)
        if (index < elements.size - 1) append(',')
        append('\n')
    }

    append(indent).append(close)
}

private fun StringBuilder.appendJsonString(value: String) {
    append('"')
    for (char in value) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }

    append('"')
}
