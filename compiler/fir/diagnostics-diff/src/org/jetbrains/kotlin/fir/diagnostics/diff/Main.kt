/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.ZonedDateTime

private const val COPIED_TEST_DATA_DIR = "testData"
private const val DIFFS_DIR = "diffs"

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
        description = "For HTML: GitHub repo and commit (separated by slashes) for links to test data. Ignored if --copyTestData is used."
    ).default("JetBrains/kotlin/master")
    private val copyTestDataOption by parser.option(
        ArgType.Boolean,
        fullName = "copyTestData",
        shortName = "c",
        description = "For HTML: Copy test data files to output. Links to test data direct there instead of GitHub."
    ).default(false)
    val input by parser.argument(
        ArgType.String,
        description = "Directory containing the diagnostic test data files, e.g., \"compiler/testData/diagnostics\""
    )
    val output by parser.argument(
        ArgType.String,
        description = "Output file name (for CSV) or directory (for HTML)"
    )

    // Should always be false if format is not HTML
    val copyTestData: Boolean
        get() = copyTestDataOption && format == Format.HTML

    val repo: String
        get() = repoAndCommit.substringBeforeLast('/')
    val commit: String
        get() = repoAndCommit.substringAfterLast('/')

    fun parse(args: Array<String>) = parser.parse(args)
}


fun main(args: Array<String>) {
    Options.parse(args)

    with(Options) {
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

        val result = DiagnosticsDiff.diffTestDataFiles(testDataDir)
        when (format) {
            Format.CSV -> outputCsv(result, outputFile)
            Format.HTML -> outputHtml(result, testDataDir, outputFile)
        }

        if (copyTestData) {
            testDataDir.walkTopDown()
                .filter { file -> file.name.endsWith(".kt") }.forEach {
                    val destFile = outputFile.resolve(COPIED_TEST_DATA_DIR).resolve(it.relativeTo(testDataDir))
                    it.copyTo(destFile)
                }
        }

        println("Diff complete! View the output ${format.name} at: ${outputFile.absolutePath}")
    }
}

private enum class Format { CSV, HTML }

private fun outputCsv(overallResult: OverallResult, outputFile: File) {
    with(overallResult) {
        outputFile.printWriter().use { out ->
            val nonMatched = missing + unexpected + mismatched
            val total = matched + nonMatched
            val overallMatchPct = matched.toDouble() / total
            out.println("diagnostic,total,matchPct,matched,nonMatched,missing,unexpected,mismatched,numFiles")
            out.println("<OVERALL>,$total,$overallMatchPct,$matched,$nonMatched,$missing,$unexpected,$mismatched,$numFiles")

            aggregateDiagnosticResults.entries
                .sortedBy { (diagnostic, _) -> diagnostic }
                .sortedByDescending { (_, result) -> result.missing.size + result.unexpected.size + result.mismatched.size }
                .forEach { (diagnostic, result) ->
                    val matchedCount = result.matched.size
                    val missingCount = result.missing.size
                    val unexpectedCount = result.unexpected.size
                    val mismatchedCount = result.mismatched.size
                    val fileCount = result.numFiles
                    val nonMatchedCount = missingCount + unexpectedCount + mismatchedCount
                    val totalCount = matchedCount + nonMatchedCount
                    val matchPct = matchedCount.toDouble() / totalCount
                    out.println("$diagnostic,$totalCount,$matchPct,$matchedCount,$nonMatchedCount,$missingCount,$unexpectedCount,$mismatchedCount,$fileCount")
                }
        }
    }
}

private fun outputHtml(
    overallResult: OverallResult,
    testDataDir: File,
    outputDir: File
) {
    with(overallResult) {
        val indexFile = outputDir.resolve("index.html")
        indexFile.printWriter().use {
            it.appendHTML().html {
                head {
                    title { +"Diagnostics diff: Summary" }
                    commonHead()
                    style {
                        unsafe {
                            +"""
                th { position: sticky; top: 0; }
                tr > *:not(:first-child) { text-align: right; }
              """.trimIndent()
                        }
                    }
                }

                body {
                    div("container") {
                        div("row") {
                            ul {
                                li {
                                    strong { +"Diff generated on: " }
                                    +ZonedDateTime.now().toString()
                                }
                                if (Options.copyTestData) {
                                    li {
                                        a(href = outputDir.resolve(COPIED_TEST_DATA_DIR).absolutePath) {
                                            target = "_blank"
                                            strong { +"Browse test data files" }
                                        }
                                    }
                                } else {
                                    li {
                                        strong { +"Diff taken at commit: " }
                                        a(href = "https://github.com/${Options.repo}/tree/${Options.commit}") {
                                            target = "_blank"
                                            +Options.commit
                                        }
                                    }
                                }
                            }
                        }
                        div("row") {
                            table("table table-bordered table-striped table-hover") {
                                thead("thead-dark") {
                                    tr {
                                        th { +"Diagnostic" }
                                        th { +"Total" }
                                        th { +"Match %" }
                                        th { +"# Matched" }
                                        th { +"# Non-matched" }
                                        th { +"# Missing" }
                                        th { +"# Unexpected" }
                                        th { +"# Mismatched" }
                                        th { +"# of Files" }
                                    }
                                }
                                tbody {
                                    val nonMatched = missing + unexpected + mismatched
                                    val total = matched + nonMatched
                                    val overallMatchPct = matched.toDouble() / total
                                    tr {
                                        td { +"<OVERALL>" }
                                        td { +"$total" }
                                        td { +"%.2f%%".format(overallMatchPct * 100) }
                                        td { +"$matched" }
                                        td { +"$nonMatched" }
                                        td { +"$missing" }
                                        td { +"$unexpected" }
                                        td { +"$mismatched" }
                                        td { +"$numFiles" }
                                    }

                                    aggregateDiagnosticResults.entries
                                        .sortedBy { (diagnostic, _) -> diagnostic }
                                        .sortedByDescending { (_, result) -> result.missing.size + result.unexpected.size + result.mismatched.size }
                                        .forEach { (diagnostic, result) ->
                                            val matchedCount = result.matched.size
                                            val missingCount = result.missing.size
                                            val unexpectedCount = result.unexpected.size
                                            val mismatchedCount = result.mismatched.size
                                            val fileCount = result.numFiles
                                            val nonMatchedCount = missingCount + unexpectedCount + mismatchedCount
                                            val totalCount = matchedCount + nonMatchedCount
                                            val matchPct = matchedCount.toDouble() / totalCount

                                            tr {
                                                td {
                                                    a(href = "$diagnostic.html") {
                                                        +diagnostic
                                                    }
                                                }
                                                td { +"$totalCount" }
                                                td { +"%.2f%%".format(matchPct * 100) }
                                                td {
                                                    a(href = "$diagnostic.html#matched") {
                                                        +"$matchedCount"
                                                    }
                                                }
                                                td { +"$nonMatchedCount" }
                                                td {
                                                    a(href = "$diagnostic.html#missing") {
                                                        +"$missingCount"
                                                    }
                                                }
                                                td {
                                                    a(href = "$diagnostic.html#unexpected") {
                                                        +"$unexpectedCount"
                                                    }
                                                }
                                                td {
                                                    a(href = "$diagnostic.html#mismatched") {
                                                        +"$mismatchedCount"
                                                    }
                                                }
                                                td { +"$fileCount" }
                                            }

                                            outputDiagnosticDetailHtml(
                                                testDataDir,
                                                outputDir,
                                                diagnostic,
                                                result
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun outputDiagnosticDetailHtml(
    testDataDir: File,
    outputDir: File,
    diagnostic: String,
    result: AggregateDiagnosticResult
) {
    fun DIV.renderList(
        testDataDir: File,
        title: String,
        list: List<SingleDiagnosticResult>,
        groupByExpectedAndActualDiagnostic: Boolean = false
    ) {
        if (list.isEmpty()) return
        div("row sticky-top") {
            style = "top: 3em; background: white"
            id = title.lowercase()
            h2 { +"$title (${list.size})" }
        }

        fun DIV.renderListSorted(list: List<SingleDiagnosticResult>) {
            div("row") {
                ul {
                    list.groupBy { it.expectedFile.absolutePath }.entries
                        .sortedBy { (path, _) -> path }
                        .sortedByDescending { (_, results) -> results.size }
                        .forEach { (_, results) ->
                            li {
                                outputViewDiffHtml(testDataDir, outputDir, results[0].expectedFile, results[0].actualFile)
                                val expectedRelativePath = results[0].expectedFile.toRelativeString(testDataDir)
                                a(href = "$DIFFS_DIR/$expectedRelativePath.html") {
                                    target = "_blank"
                                    +"$expectedRelativePath (${results.size})"
                                }
                            }
                        }
                }
            }
        }

        if (groupByExpectedAndActualDiagnostic) {
            val groupedList = list.groupBy { it.expectedRange?.diagnostic to it.actualRange?.diagnostic }.entries
            groupedList
                .sortedByDescending { (_, results) -> results.size }
                .forEach { (expectedAndActual, results) ->
                    val (expected, actual) = expectedAndActual
                    div("row sticky-top") {
                        style = "top: 5.5em; background: white"
                        h3 {
                            +"$expected -> $actual (${results.size})"
                        }
                    }
                    renderListSorted(results)
                }
        } else {
            renderListSorted(list)
        }
    }

    val detailFile = outputDir.resolve("$diagnostic.html")
    detailFile.printWriter().use {
        it.appendHTML().html {
            head {
                title { +"Diagnostics diff: $diagnostic" }
                commonHead()
            }
            body {
                div("container") {
                    div("row sticky-top") {
                        style = "background: white"
                        id = "summary"
                        div("d-inline-flex align-items-baseline") {
                            h1("mr-2") { +diagnostic }
                            a(href = "index.html") {
                                +"(back to diagnostic list)"
                            }
                        }
                    }
                    div("row") {
                        val matchedCount = result.matched.size
                        val missingCount = result.missing.size
                        val unexpectedCount = result.unexpected.size
                        val mismatchedCount = result.mismatched.size
                        val fileCount = result.numFiles
                        val nonMatchedCount = missingCount + unexpectedCount + mismatchedCount
                        val totalCount = matchedCount + nonMatchedCount
                        val matchPct = matchedCount.toDouble() / totalCount
                        ul {
                            li {
                                strong { +"Total: " }
                                +"$totalCount"
                            }
                            li {
                                strong { +"Match %: " }
                                +"%.2f%%".format(matchPct * 100)
                            }
                            li {
                                a(href = "#matched") {
                                    strong { +"# Matched: " }
                                    +"$matchedCount"
                                }
                            }
                            li {
                                strong { +"# Non-matched: " }
                                +"$nonMatchedCount"
                            }
                            li {
                                a(href = "#missing") {
                                    strong { +"# Missing: " }
                                    +"$missingCount"
                                }
                            }
                            li {
                                a(href = "#unexpected") {
                                    strong { +"# Unexpected: " }
                                    +"$unexpectedCount"
                                }
                            }
                            li {
                                a(href = "#mismatched") {
                                    strong { +"# Mismatched: " }
                                    +"$mismatchedCount"
                                }
                            }
                            li {
                                strong { +"# of Files: " }
                                +"$fileCount"
                            }
                        }
                    }

                    renderList(testDataDir, "Missing", result.missing)
                    renderList(testDataDir, "Unexpected", result.unexpected)
                    renderList(testDataDir, "Mismatched", result.mismatched, groupByExpectedAndActualDiagnostic = true)
                    renderList(testDataDir, "Matched", result.matched)
                }
            }
        }
    }
}

private fun outputViewDiffHtml(
    testDataDir: File,
    outputDir: File,
    expectedFile: File,
    actualFile: File
) {
    val expectedRelativePath = expectedFile.toRelativeString(testDataDir)
    val actualRelativePath = actualFile.toRelativeString(testDataDir)

    val diffFile = outputDir.resolve(DIFFS_DIR).resolve("$expectedRelativePath.html")
    if (diffFile.exists()) return
    diffFile.parentFile.mkdirs()

    val expectedLines = expectedFile.readLines()
    val actualLines = actualFile.readLines()

    // Setting a large context (10000) so the entire file is shown in diff
    val patch = DiffUtils.diff(expectedLines, actualLines)
    val diff = UnifiedDiffUtils.generateUnifiedDiff(expectedRelativePath, actualRelativePath, expectedLines, patch, 10000)

    // ` and $ must be escaped in JavaScript template literals
    val diffEscaped = diff.joinToString("\n").replace("`", "\\`").replace("$", "\\$")

    val repoDir = testDataDir.absolutePath.let {
        it.substring(it.indexOf("compiler/"))
    }
    val linkPrefix = if (Options.copyTestData) {
        outputDir.resolve(COPIED_TEST_DATA_DIR).absolutePath
    } else {
        "https://github.com/${Options.repo}/blob/${Options.commit}/$repoDir"
    }

    diffFile.printWriter().use {
        it.appendHTML().html {
            head {
                title { +"Diagnostics diff: $expectedRelativePath" }
                commonHead()
                link {
                    rel = "stylesheet"
                    href = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/10.7.2/styles/github.min.css"
                    integrity = "sha512-7QTQ5Qsc/IL1k8UU2bkNFjpKTfwnvGuPYE6fzm6yeneWTEGiA3zspgjcTsSgln9m0cn3MgyE7EnDNkF1bB/uCw=="
                    attributes["crossorigin"] = "anonymous"
                }
                link {
                    rel = "stylesheet"
                    href = "https://cdn.jsdelivr.net/npm/diff2html@3.4.3/bundles/css/diff2html.min.css"
                    attributes["crossorigin"] = "anonymous"
                }
                script {
                    src = "https://cdn.jsdelivr.net/npm/diff2html@3.4.3/bundles/js/diff2html-ui.min.js"
                    attributes["crossorigin"] = "anonymous"
                }
            }

            body {
                div("container-fluid") {
                    div("row") {
                        ul {
                            li {
                                a {
                                    id = "expected"
                                    target = "_blank"
                                    strong { +"Expected file: " }
                                    span { id = "expected-path" }
                                }
                            }
                            li {
                                a {
                                    id = "actual"
                                    target = "_blank"
                                    strong { +"Actual file: " }
                                    span { id = "actual-path" }
                                }
                            }
                        }
                    }
                    div("row") {
                        div {
                            id = "diff"
                            style = "width: 100%"
                        }
                    }
                }

                script(type = "text/javascript") {
                    unsafe {
                        +"""
                        const linkPrefix = '${linkPrefix}';
                        const expectedPath = '${expectedRelativePath}';
                        const actualPath = '${actualRelativePath}';
                        const expectedLinkUrl = linkPrefix + '/' + expectedPath;
                        const actualLinkUrl = linkPrefix + '/' + actualPath;
                        ${'$'}('#expected').attr('href', expectedLinkUrl);
                        ${'$'}('#actual').attr('href', actualLinkUrl);
                        ${'$'}('#expected-path').text(expectedPath);
                        ${'$'}('#actual-path').text(actualPath);

                        const diffString = `${diffEscaped}`;
                        const targetElement = ${'$'}('#diff')[0];
                        const configuration = {
                            drawFileList: false,
                            fileListToggle: false,
                            fileListStartVisible: false,
                            fileContentToggle: false,
                            matching: 'lines',
                            outputFormat: 'side-by-side',
                            synchronisedScroll: true,
                            highlight: true,
                            renderNothingWhenEmpty: false,
                        };
                        const diff2htmlUi = new Diff2HtmlUI(targetElement, diffString, configuration);
                        diff2htmlUi.draw();
                        diff2htmlUi.highlightCode();
                        """.trimIndent()
                    }
                }
            }
        }
    }
}

private fun HEAD.commonHead() {
    meta { charset = "utf-8" }
    script(src = "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js") {
        integrity =
            "sha512-894YE6QWD5I59HgZOGReFYm4dnWc1Qt5NtvYSaNcOP+u1T9qYdvdihz0PPSiiqn/+/3e7Jo4EaG7TubfWGUrMQ=="
        attributes["crossorigin"] = "anonymous"
    }
    link {
        rel = "stylesheet"
        href = "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.6.0/css/bootstrap.min.css"
        integrity =
            "sha512-P5MgMn1jBN01asBgU0z60Qk4QxiXo86+wlFahKrsQf37c9cro517WzVSPPV1tDKzhku2iJ2FVgL67wG03SGnNA=="
        attributes["crossorigin"] = "anonymous"
    }
}
