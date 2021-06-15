/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File

internal fun HTML.outputIndex(overallResult: OverallResult, isDelta: Boolean = false, outputHeader: DIV.() -> Unit) {
    with(overallResult) {
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
                    outputHeader()
                }
                div("row") {
                    table("table table-bordered table-striped table-hover") {
                        fun Int.formatted() = formatted(isDelta)

                        thead("thead-dark") {
                            tr {
                                val symbol = if (isDelta) "Δ" else "#"
                                th { +"Diagnostic" }
                                th { +"Total" }
                                th { +"${if (isDelta) "Δ " else ""}Match %" }
                                th { +"$symbol Matched" }
                                th { +"$symbol Non-matched" }
                                th { +"$symbol Missing" }
                                th { +"$symbol Unexpected" }
                                th { +"$symbol Mismatched" }
                                th { +"$symbol Files" }
                            }
                        }
                        tbody {
                            tr {
                                td { +"<OVERALL>" }
                                td { +total.formatted() }
                                td { +matchPct.toPercent() }
                                td { +matched.formatted() }
                                td { +nonMatched.formatted() }
                                td { +missing.formatted() }
                                td { +unexpected.formatted() }
                                td { +mismatched.formatted() }
                                td { +numFiles.formatted() }
                            }

                            for (diagnostic in allDiagnosticsSortedByNonMatchedDescending) {
                                val matchedCount = matchedFor(diagnostic)
                                val missingCount = missingFor(diagnostic)
                                val unexpectedCount = unexpectedFor(diagnostic)
                                val mismatchedCount = mismatchedFor(diagnostic)
                                val fileCount = numFilesFor(diagnostic)
                                val nonMatchedCount = nonMatchedFor(diagnostic)
                                val totalCount = totalFor(diagnostic)
                                val matchPct = matchPctFor(diagnostic)

                                tr {
                                    td {
                                        a(href = "$diagnostic.html") {
                                            +diagnostic
                                        }
                                    }
                                    td { +totalCount.formatted() }
                                    td { +matchPct.toPercent() }
                                    td {
                                        a(href = "$diagnostic.html#matched") {
                                            +matchedCount.formatted()
                                        }
                                    }
                                    td { +nonMatchedCount.formatted() }
                                    td {
                                        a(href = "$diagnostic.html#missing") {
                                            +missingCount.formatted()
                                        }
                                    }
                                    td {
                                        a(href = "$diagnostic.html#unexpected") {
                                            +unexpectedCount.formatted()
                                        }
                                    }
                                    td {
                                        a(href = "$diagnostic.html#mismatched") {
                                            +mismatchedCount.formatted()
                                        }
                                    }
                                    td { +fileCount.formatted() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun DIV.outputDiagnosticDetailSummary(
    overallResult: OverallResult,
    diagnostic: String,
    isDelta: Boolean = false,
    externalLinkPrefix: String? = null
) {
    val matchedCount = overallResult.matchedFor(diagnostic)
    val missingCount = overallResult.missingFor(diagnostic)
    val unexpectedCount = overallResult.unexpectedFor(diagnostic)
    val mismatchedCount = overallResult.mismatchedFor(diagnostic)
    val fileCount = overallResult.numFilesFor(diagnostic)
    val nonMatchedCount = overallResult.nonMatchedFor(diagnostic)
    val totalCount = overallResult.totalFor(diagnostic)
    val matchPct = overallResult.matchPctFor(diagnostic)

    fun Int.formatted() = formatted(isDelta)

    ul {
        val symbol = if (isDelta) "Δ" else "#"
        val detailPageUrl = if (externalLinkPrefix != null) "$externalLinkPrefix/$diagnostic.html" else ""
        li {
            strong { +"Total: " }
            +totalCount.formatted()
        }
        li {
            strong { +"Match %: " }
            +matchPct.toPercent()
        }
        li {
            a(href = "$detailPageUrl#matched") {
                if (externalLinkPrefix != null) {
                    target = "_blank"
                }
                strong { +"$symbol Matched: " }
                +matchedCount.formatted()
            }
        }
        li {
            strong { +"$symbol Non-matched: " }
            +nonMatchedCount.formatted()
        }
        li {
            a(href = "$detailPageUrl#missing") {
                if (externalLinkPrefix != null) {
                    target = "_blank"
                }
                strong { +"$symbol Missing: " }
                +missingCount.formatted()
            }
        }
        li {
            a(href = "$detailPageUrl#unexpected") {
                if (externalLinkPrefix != null) {
                    target = "_blank"
                }
                strong { +"$symbol Unexpected: " }
                +unexpectedCount.formatted()
            }
        }
        li {
            a(href = "$detailPageUrl#mismatched") {
                if (externalLinkPrefix != null) {
                    target = "_blank"
                }
                strong { +"$symbol Mismatched: " }
                +mismatchedCount.formatted()
            }
        }
        li {
            strong { +"$symbol Files: " }
            +fileCount.formatted()
        }
    }
}

internal fun outputViewDiffHtml(
    testRelativePath: String,
    expectedFile: File,
    expectedDisplayName: String,
    expectedLink: String,
    actualFile: File,
    actualDisplayName: String,
    actualLink: String,
    diffFile: File
) {
    val expectedLines = if (expectedFile.exists()) expectedFile.readLines() else listOf()
    val actualLines = if (actualFile.exists()) actualFile.readLines() else listOf()

    // Setting a large context (10000) so the entire file is shown in diff
    val patch = DiffUtils.diff(expectedLines, actualLines)
    val diff = UnifiedDiffUtils.generateUnifiedDiff(expectedDisplayName, actualDisplayName, expectedLines, patch, 10000)

    // ` and $ must be escaped in JavaScript template literals
    val diffEscaped = diff.joinToString("\n").replace("`", "\\`").replace("$", "\\$")

    diffFile.printWriter().use {
        it.appendHTML().html {
            head {
                title { +"Diagnostics diff: $testRelativePath" }
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
                        ${'$'}('#expected').attr('href', '${expectedLink}');
                        ${'$'}('#actual').attr('href', '${actualLink}');
                        ${'$'}('#expected-path').text('${expectedDisplayName}');
                        ${'$'}('#actual-path').text('${actualDisplayName}');

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

internal fun HEAD.commonHead() {
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

private fun Double.toPercent(): String = if (isInfinite() || isNaN()) "N/A" else "%.2f%%".format(this * 100)

internal fun Int.formatted(isDelta: Boolean): String = if (isDelta && this > 0) "+$this" else this.toString()