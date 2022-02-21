/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.ZonedDateTime

class SingleCommitReportGenerator(
    private val overallResult: SingleCommitOverallResult,
    private val outputDir: File,
    private val testDataDir: File,
    private val repo: String?,
    private val commit: String?,
    private val copiedTestDataLink: String?
) {
    fun generateReport() {
        val indexFile = outputDir.resolve("index.html")
        indexFile.printWriter().use {
            it.appendHTML().html {
                outputIndex(overallResult) {
                    ul {
                        li {
                            strong { +"Report generated on: " }
                            +ZonedDateTime.now().toString()
                        }
                        if (repo != null) {
                            li {
                                strong { +"Diff taken at commit: " }
                                a(href = "https://github.com/$repo/tree/${commit!!}") {
                                    target = "_blank"
                                    +commit
                                }
                            }
                        } else {
                            requireNotNull(copiedTestDataLink)
                            li {
                                a(href = copiedTestDataLink) {
                                    target = "_blank"
                                    strong { +"Browse test data files" }
                                }
                            }
                        }
                    }
                }
            }
        }

        for ((diagnostic, result) in overallResult.aggregateDiagnosticResults.entries) {
            outputDiagnosticDetailHtml(diagnostic, result)
        }
    }

    private fun outputDiagnosticDetailHtml(
        diagnostic: String,
        result: AggregateDiagnosticResult
    ) {
        fun DIV.renderList(
            title: String,
            list: Collection<SingleDiagnosticResult>,
            groupByExpectedAndActualDiagnostic: Boolean = false
        ) {
            if (list.isEmpty()) return
            div("row sticky-top") {
                style = "top: 3em; background: white"
                id = title.lowercase()
                h2 { +"$title (${list.size})" }
            }

            fun DIV.renderListSorted(list: Collection<SingleDiagnosticResult>) {
                div("row") {
                    ul {
                        list.groupBy { it.expectedFile.absolutePath }.entries
                            .sortedBy { (path, _) -> path }
                            .sortedByDescending { (_, results) -> results.size }
                            .forEach { (_, results) ->
                                li {
                                    val expectedRelativePath = results[0].expectedFile.toRelativeString(testDataDir)
                                    val actualRelativePath = results[0].actualFile.toRelativeString(testDataDir)
                                    val expectedFileNormalized = expectedRelativePath.replace("/", "__")
                                    val diffFile = outputDir.resolve("$DIFF_HTML_FILE_PREFIX$expectedFileNormalized.html")
                                    val repoDir = testDataDir.absolutePath.let {
                                        it.substring(it.indexOf("compiler/"))
                                    }
                                    val linkPrefix =
                                        if (repo != null) "https://github.com/$repo/blob/${commit!!}/$repoDir" else copiedTestDataLink!!
                                    val expectedLink = "$linkPrefix/$expectedRelativePath"
                                    val actualLink = "$linkPrefix/$actualRelativePath"

                                    diffFile.outputViewDiffHtml(
                                        expectedRelativePath,
                                        results[0].expectedFile,
                                        expectedRelativePath,
                                        expectedLink,
                                        results[0].actualFile,
                                        actualRelativePath,
                                        actualLink
                                    )
                                    a(href = diffFile.name) {
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
                            h5 {
                                +"$expected → $actual (${results.size})"
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
                            outputDiagnosticDetailSummary(overallResult, diagnostic)
                        }

                        renderList("Missing", result.missing)
                        renderList("Unexpected", result.unexpected)
                        renderList("Mismatched", result.mismatched, groupByExpectedAndActualDiagnostic = true)
                        renderList("Matched", result.matched)
                    }
                }
            }
        }
    }

    companion object {
        private const val DIFF_HTML_FILE_PREFIX = "diff-"
    }
}