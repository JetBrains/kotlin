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

internal data class DiffHtmlInput(
    val diffTitle: String,
    val leftFile: File,
    val leftFilename: String,
    val leftLink: String,
    val leftLabel: String,
    val rightFile: File,
    val rightFilename: String,
    val rightLink: String,
    val rightLabel: String
)

internal fun File.outputViewDiffHtml(
    pathForPageTitle: String,
    leftFile: File,
    leftFilename: String,
    leftLink: String,
    rightFile: File,
    rightFilename: String,
    rightLink: String,
    diffTitle: String = "Diff between FE 1.0 vs FIR",
    leftLabel: String = "FE 1.0",
    rightLabel: String = "FIR"
) {
    outputViewDiffHtml(
        pathForPageTitle,
        listOf(DiffHtmlInput(diffTitle, leftFile, leftFilename, leftLink, leftLabel, rightFile, rightFilename, rightLink, rightLabel))
    )
}

@OptIn(ExperimentalStdlibApi::class)
internal fun File.outputViewDiffHtml(
    pathForTitle: String,
    inputs: List<DiffHtmlInput>
) {
    fun DIV.renderDiff(index: Int, input: DiffHtmlInput) {
        val (diffTitle, leftFile, leftFilename, leftLink, leftLabel, rightFile, rightFilename, rightLink, rightLabel) = input
        val leftLines = if (leftFile.exists()) leftFile.readLines() else listOf()
        val rightLines = if (rightFile.exists()) rightFile.readLines() else listOf()

        // Setting a large context (10000) so the entire file is shown in diff
        val patch = DiffUtils.diff(leftLines, rightLines)
        val diff = if (patch.deltas.isEmpty()) {
            // Files are identical. Nothing will render if we pass in an empty patch to Diff2Html, so create a "diff" with the whole file.
            assert(leftLines == rightLines)
            buildList {
                add("--- $leftFilename")
                add("+++ $rightFilename")
                val lineCount = leftLines.size
                add("@@ -1,$lineCount +1,$lineCount @@")
                // Unified diff requires single space before identical lines
                addAll(leftLines.map { " $it" })
            }
        } else {
            UnifiedDiffUtils.generateUnifiedDiff(leftFilename, rightFilename, leftLines, patch, 10000)
        }

        // ` and $ must be escaped in JavaScript template literals
        val diffEscaped = diff.joinToString("\n").replace("`", "\\`").replace("$", "\\$")

        div("card-header p-0") {
            id = "heading${index}"
            button(classes = "btn btn-link btn-block text-left") {
                attributes["data-toggle"] = "collapse"
                attributes["data-target"] = "#collapse${index}"
                h5 {
                    +diffTitle
                }
            }
        }
        div(if (index == 0) "collapse show" else "collapse") {
            id = "collapse${index}"
            div("card-body p-1") {
                div {
                    id = "diff${index}"
                    style = "width: 100%"
                }
            }
        }
        script(type = "text/javascript") {
            unsafe {
                +"""
                (function() {
                    const diffString = `${diffEscaped}`;
                    const targetElement = ${'$'}('#diff${index}')[0];
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
                        rawTemplates: {
                            'generic-file-path': `
                                <div class="col">
                                    <a href="${leftLink}" target="_blank">
                                        <strong>${leftLabel}:</strong> ${leftFilename}
                                    </a>
                                </div>
                                <div class="col">
                                    <a href="${rightLink}" target="_blank">
                                        <strong>${rightLabel}:</strong> ${rightFilename}
                                    </a>
                                </div>
                            `
                        }
                    };
                    const diff2htmlUi = new Diff2HtmlUI(targetElement, diffString, configuration);
                    diff2htmlUi.draw();
                    diff2htmlUi.highlightCode();
                })();
                """.trimIndent()
            }
        }
    }

    printWriter().use {
        it.appendHTML().html {
            head {
                title { +"Diagnostics diff: $pathForTitle" }
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
                    inputs.forEachIndexed { index, diff -> renderDiff(index, diff) }
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
    script(src = "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.6.0/js/bootstrap.min.js") {
        integrity =
            "sha512-XKa9Hemdy1Ui3KSGgJdgMyYlUg1gM+QhL6cnlyTe2qzMCYm4nAZ1PsVerQzTTXzonUR+dmswHqgJPuwCq1MaAg=="
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