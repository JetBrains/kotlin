/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import java.util.concurrent.TimeUnit

private const val EXEC_TIMEOUT_SECONDS = 10L

private object Options {
    private val parser = ArgParser("diagnostics-diff")

    val format by parser.option(
        ArgType.Choice<Format>(),
        fullName = "format",
        shortName = "f",
        description = "Format for output"
    ).default(Format.CSV)
    val repo by parser.option(
        ArgType.String,
        fullName = "repo",
        shortName = "r",
        description = "For HTML: GitHub repo for links to test data. " +
                "If not specified, the test data is copied to output and links direct there."
    )
    val beforeRev by parser.option(
        ArgType.String,
        fullName = "before",
        shortName = "b",
        description = "Git revision to use as for the \"before\" commit (i.e., left side of diff)."
    ).default("HEAD~")
    val afterRev by parser.option(
        ArgType.String,
        fullName = "after",
        shortName = "a",
        description = "Git revision to use as for the \"after\" commit (i.e., right side of diff)."
    ).default("HEAD")
    val input by parser.argument(
        ArgType.String,
        description = "Directory containing the diagnostic test data files, e.g., \"compiler/testData/diagnostics\""
    )
    val output by parser.argument(
        ArgType.String,
        description = "Output file name (for CSV) or directory (for HTML)"
    )

    fun parse(args: Array<String>) = parser.parse(args)
}

fun main(args: Array<String>) {
    with(Options) {
        parse(args)
        val testDataDir = File(input)
        check(testDataDir.exists()) { "Test data directory $input does not exist" }
        check(testDataDir.isDirectory) { "Test data directory $input is not a directory" }
        val outputFile = File(output)

        if (format == Format.HTML) {
            check(!outputFile.exists() || outputFile.isDirectory) { "Output directory ${outputFile.absolutePath} exists and is not a directory" }
            if (outputFile.isDirectory) {
                println("Deleting existing output directory ${outputFile.absolutePath}")
                check(outputFile.deleteRecursively()) { "Unable to delete output directory ${outputFile.absolutePath}" }
            }
            outputFile.mkdirs()
        }

        val originalCommit = getParsedRevision()
        val originalBranch = getCheckedOutBranch()
        val originalRev = if (originalBranch.isNotEmpty()) originalBranch else originalCommit
        val afterCommit = getParsedRevision(afterRev)
        val beforeCommit = getParsedRevision(beforeRev)
        println("Currently checked out revision: $originalRev")
        println("Before commit: $beforeCommit")
        println("After commit: $afterCommit")

        fun generateReport(result: SingleCommitOverallResult, outputFile: File, commit: String, copiedTestDataLink: String?) {
            with(Options) {
                when (format) {
                    Format.CSV -> CsvReportGenerator(result, outputFile).generateReport()
                    Format.HTML -> SingleCommitReportGenerator(
                        result,
                        outputFile,
                        testDataDir,
                        repo,
                        commit,
                        copiedTestDataLink
                    ).generateReport()
                }
            }
        }

        try {
            val extensionIfAny = outputFile.extension.let { if (it.isEmpty()) "" else ".$it" }

            fun getOutputFileFor(label: String) = when (format) {
                Format.CSV -> File(outputFile.parent, "${outputFile.nameWithoutExtension}-$label$extensionIfAny")
                Format.HTML -> File(outputFile, label).also { it.mkdirs() }
            }

            checkoutRevision(afterRev)
            val afterOutputFile = getOutputFileFor("after")
            val afterResult = DiagnosticsDiff.diffTestDataFiles(testDataDir)
            val afterCopiedTestDataDir = if (format == Format.HTML) copyTestData(testDataDir, afterOutputFile) else null
            generateReport(afterResult, afterOutputFile, afterCommit, afterCopiedTestDataDir?.path)

            checkoutRevision(beforeRev)
            val beforeOutputFile = getOutputFileFor("before")
            val beforeResult = DiagnosticsDiff.diffTestDataFiles(testDataDir)
            val beforeCopiedTestDataDir = if (format == Format.HTML) copyTestData(testDataDir, beforeOutputFile) else null
            generateReport(beforeResult, beforeOutputFile, beforeCommit, beforeCopiedTestDataDir?.path)

            val deltaResult = DeltaOverallResult(beforeResult, afterResult)
            when (format) {
                Format.CSV -> CsvReportGenerator(deltaResult, outputFile).generateReport()
                Format.HTML -> DeltaHtmlReportGenerator(
                    deltaResult,
                    outputFile,
                    testDataDir,
                    repo,
                    beforeResult,
                    beforeCommit,
                    beforeOutputFile,
                    beforeCopiedTestDataDir!!,
                    afterResult,
                    afterCommit,
                    afterOutputFile,
                    afterCopiedTestDataDir!!
                ).generateReport()
            }

            if (repo != null) {
                // We only needed the copied test data to generate the diff pages in the report. The links will point to GitHub,
                // so it should be safe to delete the copied test data.
                afterCopiedTestDataDir?.deleteRecursively()
                beforeCopiedTestDataDir?.deleteRecursively()
            }
        } finally {
            // Restore original branch or commit
            println("Restoring original checked out revision: $originalRev")
            checkoutRevision(originalRev)
        }
    }
}

private fun getCheckedOutBranch() = exec("git", "branch", "--show-current")

private fun getParsedRevision(rev: String = "HEAD"): String {
    val commit = exec("git", "rev-parse", rev)
    if (commit.isEmpty()) {
        throw RuntimeException("Unable to parse revision")
    }
    return commit
}

private fun checkoutRevision(revision: String) {
    exec("git", "checkout", revision)
}

private fun exec(vararg args: String): String {
    val process = ProcessBuilder(*args).start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val finished = process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    if (finished) {
        val exitValue = process.exitValue()
        if (exitValue != 0) {
            throw RuntimeException("`${args.joinToString(" ")}` exited with code: $exitValue")
        }
    } else {
        process.destroy()
        throw RuntimeException("`${args.joinToString(" ")}` did not finish in $EXEC_TIMEOUT_SECONDS seconds")
    }
    return output.trim()
}

private enum class Format { CSV, HTML }

private fun copyTestData(testDataDir: File, outputDir: File): File {
    val copiedTestDataDir = outputDir.resolve(COPIED_TEST_DATA_DIR)
    testDataDir.walkTopDown()
        .filter { file -> file.name.endsWith(".kt") }.forEach {
            val destFile = copiedTestDataDir.resolve(it.relativeTo(testDataDir))
            it.copyTo(destFile)
        }
    return copiedTestDataDir
}

private const val COPIED_TEST_DATA_DIR = "testData"
