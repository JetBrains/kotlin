/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.ZonedDateTime

class DeltaHtmlReportGenerator(
    private val deltaResult: DeltaOverallResult,
    private val outputDir: File,
    private val testDataDir: File,
    private val repo: String?,
    private val beforeResult: SingleCommitOverallResult,
    private val beforeCommit: String,
    private val beforeReportDir: File,
    private val beforeCopiedTestDataDir: File,
    private val afterResult: SingleCommitOverallResult,
    private val afterCommit: String,
    private val afterReportDir: File,
    private val afterCopiedTestDataDir: File
) {
    fun generateReport() {
        val indexFile = outputDir.resolve("index.html")
        indexFile.printWriter().use {
            it.appendHTML().html {
                outputIndex(deltaResult, isDelta = true) {
                    ul {
                        li {
                            strong { +"Report generated on: " }
                            +ZonedDateTime.now().toString()
                        }
                        li {
                            strong { +"Before commit: " }
                            if (repo != null) {
                                a(href = "https://github.com/$repo/tree/$beforeCommit") {
                                    target = "_blank"
                                    +beforeCommit
                                }
                            } else {
                                a(href = beforeCopiedTestDataDir.toRelativeString(outputDir)) {
                                    target = "_blank"
                                    +beforeCommit
                                }
                            }
                            +" "
                            a(href = beforeReportDir.resolve("index.html").toRelativeString(outputDir)) {
                                target = "_blank"
                                +"(View report)"
                            }
                        }
                        li {
                            strong { +"After commit: " }
                            if (repo != null) {
                                a(href = "https://github.com/$repo/tree/$afterCommit") {
                                    target = "_blank"
                                    +afterCommit
                                }
                            } else {
                                a(href = afterCopiedTestDataDir.toRelativeString(outputDir)) {
                                    target = "_blank"
                                    +afterCommit
                                }
                            }
                            +" "
                            a(href = afterReportDir.resolve("index.html").toRelativeString(outputDir)) {
                                target = "_blank"
                                +"(View report)"
                            }
                        }
                    }
                }
            }
        }

        for (diagnostic in deltaResult.allDiagnostics) {
            outputDiagnosticDetailHtml(diagnostic)
        }
    }

    private fun outputDiagnosticDetailHtml(diagnostic: String) {
        fun DIV.renderLists(
            title: String,
            removedList: Collection<SingleDiagnosticResult>?,
            addedList: Collection<SingleDiagnosticResult>?,
            groupByExpectedAndActualDiagnostic: Boolean = false
        ) {
            if (removedList.isNullOrEmpty() && addedList.isNullOrEmpty()) return
            div("row sticky-top") {
                style = "top: 3em; background: white; z-index: 200"
                id = title.lowercase()
                val totalCount = ((addedList?.size ?: 0) - (removedList?.size ?: 0)).formatted(isDelta = true)
                h2 { +"$title ($totalCount)" }
            }

            fun DIV.renderListSorted(header: String, list: Collection<SingleDiagnosticResult>, isRemovedList: Boolean) {
                div("col") {
                    div("row sticky-top") {
                        val top = if (groupByExpectedAndActualDiagnostic) "7.5em" else "5.75em"
                        style = "top: $top; background: white; z-index: 100"
                        val totalCount = (if (isRemovedList) -list.size else list.size).formatted(isDelta = true)
                        h6 { +"$header ($totalCount)" }
                    }
                    div("row")
                    ul {
                        list.groupBy { it.expectedFile.absolutePath }.entries
                            .sortedBy { (path, _) -> path }
                            .sortedByDescending { (_, results) -> results.size }
                            .forEach { (_, results) ->
                                li {
                                    // It is possible the FIR file could be missing in either "before" or "after", if the test is or became
                                    // "FIR_IDENTICAL".
                                    fun File.preferFirFile(): File {
                                        return if (name.endsWith(".fir.kt")) {
                                            if (exists()) this else File(parentFile, name.replace(".fir.kt", ".kt"))
                                        } else {
                                            File(parentFile, "$nameWithoutExtension.fir.kt").takeIf { it.exists() } ?: this
                                        }
                                    }

                                    // Show diffs between:
                                    // 1. "FIR before" and "FIR after"
                                    // 2. "FE 1.0 after" and "FIR after"
                                    // 3. "FE 1.0 before" and "FIR before"

                                    val testRelativePath = results[0].expectedFile.toRelativeString(testDataDir)
                                    val beforeFE10File = beforeCopiedTestDataDir.resolve(testRelativePath)
                                    val beforeFE10RelativePath = beforeFE10File.toRelativeString(outputDir)
                                    val beforeFirFile = beforeFE10File.preferFirFile()
                                    val beforeFirRelativePath = beforeFirFile.toRelativeString(outputDir)
                                    val afterFE10File = afterCopiedTestDataDir.resolve(testRelativePath)
                                    val afterFE10RelativePath = afterFE10File.toRelativeString(outputDir)
                                    val afterFirFile = afterFE10File.preferFirFile()
                                    val afterFirRelativePath = afterFirFile.toRelativeString(outputDir)

                                    val repoDir = testDataDir.absolutePath.let {
                                        it.substring(it.indexOf("compiler/"))
                                    }
                                    val beforeFE10Link = if (repo != null) {
                                        "https://github.com/$repo/blob/$beforeCommit/$repoDir/${
                                            beforeFE10File.toRelativeString(
                                                beforeCopiedTestDataDir
                                            )
                                        }"
                                    } else beforeFE10RelativePath
                                    val beforeFirLink = if (repo != null) {
                                        "https://github.com/$repo/blob/$beforeCommit/$repoDir/${
                                            beforeFirFile.toRelativeString(
                                                beforeCopiedTestDataDir
                                            )
                                        }"
                                    } else beforeFirRelativePath
                                    val afterFE10Link = if (repo != null) {
                                        "https://github.com/$repo/blob/$afterCommit/$repoDir/${
                                            afterFE10File.toRelativeString(
                                                afterCopiedTestDataDir
                                            )
                                        }"
                                    } else afterFE10RelativePath
                                    val afterFirLink = if (repo != null) {
                                        "https://github.com/$repo/blob/$afterCommit/$repoDir/${
                                            afterFirFile.toRelativeString(
                                                afterCopiedTestDataDir
                                            )
                                        }"
                                    } else afterFirRelativePath

                                    val firBeforeVsFirAfterInput = DiffHtmlInput(
                                        "Diff between FIR before vs FIR after",
                                        beforeFirFile,
                                        beforeFirRelativePath,
                                        beforeFirLink,
                                        "FIR before",
                                        afterFirFile,
                                        afterFirRelativePath,
                                        afterFirLink,
                                        "FIR after"
                                    )
                                    val fe10AfterVsFirAfterInput = DiffHtmlInput(
                                        "Diff between FE 1.0 after vs FIR after",
                                        afterFE10File,
                                        afterFE10RelativePath,
                                        afterFE10Link,
                                        "FE 1.0 after",
                                        afterFirFile,
                                        afterFirRelativePath,
                                        afterFirLink,
                                        "FIR after"
                                    )
                                    val fe10BeforeVsFirBeforeInput = DiffHtmlInput(
                                        "Diff between FE 1.0 before vs FIR before",
                                        beforeFE10File,
                                        beforeFE10RelativePath,
                                        beforeFE10Link,
                                        "FE 1.0 before",
                                        beforeFirFile,
                                        beforeFirRelativePath,
                                        beforeFirLink,
                                        "FIR before"
                                    )

                                    val testFileNormalized = testRelativePath.replace("/", "__")
                                    val diffFile = outputDir.resolve("$DIFF_HTML_FILE_PREFIX$testFileNormalized.html")
                                    diffFile.outputViewDiffHtml(
                                        testRelativePath,
                                        listOf(firBeforeVsFirAfterInput, fe10AfterVsFirAfterInput, fe10BeforeVsFirBeforeInput)
                                    )
                                    a(href = diffFile.name) {
                                        target = "_blank"
                                        val fileCount = (if (isRemovedList) -results.size else results.size).formatted(isDelta = true)
                                        +"$testRelativePath ($fileCount)"
                                    }
                                }
                            }
                    }
                }
            }

            fun DIV.renderListsSorted(removedList: Collection<SingleDiagnosticResult>?, addedList: Collection<SingleDiagnosticResult>?) {
                div("row") {
                    renderListSorted("Removed", removedList ?: emptyList(), isRemovedList = true)
                    renderListSorted("Added", addedList ?: emptyList(), isRemovedList = false)
                }
            }

            if (groupByExpectedAndActualDiagnostic) {
                val groupedRemovedList = removedList?.groupBy { it.expectedRange?.diagnostic to it.actualRange?.diagnostic } ?: emptyMap()
                val groupedAddedList = addedList?.groupBy { it.expectedRange?.diagnostic to it.actualRange?.diagnostic } ?: emptyMap()
                val allExpectedAndActual = groupedRemovedList.keys + groupedAddedList.keys
                allExpectedAndActual
                    .associateWith { (groupedRemovedList[it]?.size ?: 0) + (groupedAddedList[it]?.size ?: 0) }
                    .entries.sortedByDescending { (_, size) -> size }
                    .forEach { (expectedAndActual, size) ->
                        val (expected, actual) = expectedAndActual
                        div("row sticky-top") {
                            style = "top: 5.5em; background: white"
                            h5 {
                                +"$expected → $actual (${size.formatted(isDelta = true)})"
                            }
                        }
                        renderListsSorted(groupedRemovedList[expectedAndActual], groupedAddedList[expectedAndActual])
                    }
            } else {
                renderListsSorted(removedList, addedList)
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
                            div("col") {
                                div("row") {
                                    h4 {
                                        +"Delta:"
                                    }
                                }
                                div("row") {
                                    outputDiagnosticDetailSummary(deltaResult, diagnostic, isDelta = true)
                                }
                            }
                            div("col") {
                                div("row") {
                                    h4 {
                                        +"Before:"
                                    }
                                }
                                div("row") {
                                    outputDiagnosticDetailSummary(
                                        beforeResult,
                                        diagnostic,
                                        externalLinkPrefix = beforeReportDir.toRelativeString(outputDir)
                                    )
                                }
                            }
                            div("col") {
                                div("row") {
                                    h4 {
                                        +"After:"
                                    }
                                }
                                div("row") {
                                    outputDiagnosticDetailSummary(
                                        afterResult,
                                        diagnostic,
                                        externalLinkPrefix = afterReportDir.toRelativeString(outputDir)
                                    )
                                }
                            }
                        }

                        renderLists("Missing", deltaResult.removed[diagnostic]?.missing, deltaResult.added[diagnostic]?.missing)
                        renderLists("Unexpected", deltaResult.removed[diagnostic]?.unexpected, deltaResult.added[diagnostic]?.unexpected)
                        renderLists(
                            "Mismatched",
                            deltaResult.removed[diagnostic]?.mismatched,
                            deltaResult.added[diagnostic]?.mismatched,
                            groupByExpectedAndActualDiagnostic = true
                        )
                        renderLists("Matched", deltaResult.removed[diagnostic]?.matched, deltaResult.added[diagnostic]?.matched)
                    }
                }
            }
        }
    }

    companion object {
        private const val DIFF_HTML_FILE_PREFIX = "diff-"
    }
}