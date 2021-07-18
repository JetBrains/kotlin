/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

private object Options {
    private val parser = ArgParser("diagnostics-diff")

    val format by parser.option(
        ArgType.Choice<Format>(),
        fullName = "format",
        shortName = "f",
        description = "Format for output"
    ).default(Format.CSV)
    private val repoAndCommit by parser.option(
        ArgType.String,
        fullName = "repoAndCommit",
        shortName = "r",
        description = "For HTML: GitHub repo and commit (separated by slashes) for links to test data. " +
                "If not specified, the test data is copied to output and links direct there."
    )
    val input by parser.argument(
        ArgType.String,
        description = "Directory containing the diagnostic test data files, e.g., \"compiler/testData/diagnostics\""
    )
    val output by parser.argument(
        ArgType.String,
        description = "Output file name (for CSV) or directory (for HTML)"
    )

    val repo: String?
        get() = repoAndCommit?.substringBeforeLast('/')
    val commit: String?
        get() = repoAndCommit?.substringAfterLast('/')

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

            if (repo != null) {
                testDataDir.walkTopDown()
                    .filter { file -> file.name.endsWith(".kt") }.forEach {
                        val destFile = outputFile.resolve(COPIED_TEST_DATA_DIR).resolve(it.relativeTo(testDataDir))
                        it.copyTo(destFile)
                    }
            }
        }

        val result = DiagnosticsDiff.diffTestDataFiles(testDataDir)
        when (format) {
            Format.CSV -> CsvReportGenerator(result, outputFile).generateReport()
            Format.HTML -> SingleCommitReportGenerator(
                result,
                outputFile,
                testDataDir,
                repo,
                commit,
                COPIED_TEST_DATA_DIR
            ).generateReport()
        }

        println("Diff complete! View the output ${format.name} at: ${outputFile.absolutePath}")
    }
}

private enum class Format { CSV, HTML }

private const val COPIED_TEST_DATA_DIR = "testData"
