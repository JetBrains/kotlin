/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

abstract class AllKotlinTest : DaemonAnalyzerTestCase() {

    private val rootProjectFile: File = File(".").absoluteFile
    private val statsFile: File = File("build/stats.csv").absoluteFile
    private val tmp by lazy { createTempDirectory() }

    override fun setUpProject() {

        println("Copying project to $tmp")

        rootProjectFile.copyRecursively(tmp)
        tmp.resolve(".idea").deleteRecursively()

        (ApplicationManager.getApplication() as ApplicationEx).doNotSave()
        myProject = ProjectUtil.openOrImport(tmp.path, null, false)
    }

    data class PerFileTestResult(val perProcess: Map<String, Long>, val totalNs: Long, val errors: List<Throwable>)

    protected abstract fun doTest(file: File): PerFileTestResult

    protected fun File.toPsiFile(): PsiFile? {
        val vFile = VfsUtil.findFileByIoFile(this, false) ?: return null

        val psi = PsiManager.getInstance(project)
        return psi.findFile(vFile)
    }

    fun testWholeProjectPerformance() {

        val totals = mutableMapOf<String, Long>()

        val statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln("File, InspectionID, Time")

        fun appendInspectionResult(file: String, id: String, nanoTime: Long) {
            totals.merge(id, nanoTime) { a, b -> a + b }

            statsOutput.appendln(buildString {
                append(file)
                append(", ")
                append(id)
                append(", ")
                append((nanoTime * 1e-6).toLong())
            })
        }

        tcSuite("SumPerFile") {
            tmp.walkTopDown().filter {
                it.extension == "kt" && "testdata" !in it.path.toLowerCase() && "resources" !in it.path
            }.forEach {
                val filePath = it.relativeTo(tmp).path.replace(File.separatorChar, '/')
                tcTest(filePath) {
                    val result = doTest(it)
                    result.perProcess.forEach { (k, v) ->
                        appendInspectionResult(filePath, k, v)
                    }
                    (result.totalNs * 1e-6).toLong() to result.errors
                }
            }
        }

        statsOutput.flush()
        statsOutput.close()


        tcSuite("Total") {
            totals.forEach { (k, v) ->
                tcTest(k) {
                    (v * 1e-6).toLong() to emptyList()
                }
            }
        }

    }

    private inline fun tcSuite(name: String, block: () -> Unit) {
        println("##teamcity[testSuiteStarted name='$name']")
        block()
        println("##teamcity[testSuiteFinished name='$name']")
    }

    private inline fun tcTest(name: String, block: () -> Pair<Long, List<Throwable>>) {
        println("##teamcity[testStarted name='$name' captureStandardOutput='true']")
        val (time, errors) = block()
        if (errors.isNotEmpty()) {
            val detailsWriter = StringWriter()
            val errorDetailsPrintWriter = PrintWriter(detailsWriter)
            errors.forEach {
                it.printStackTrace(errorDetailsPrintWriter)
                errorDetailsPrintWriter.println()
            }
            errorDetailsPrintWriter.close()
            val details = detailsWriter.toString()
            println("##teamcity[testFailed name='$name' message='Exceptions reported' details='${details.tcEscape()}']")
        }
        println("##teamcity[testFinished name='$name' duration='$time']")
    }

    private fun String.tcEscape(): String {
        return this
            .replace("|", "||")
            .replace("[", "|[")
            .replace("]", "|]")
            .replace("\r", "|r")
            .replace("\n", "|n")
            .replace("'", "|'")
    }

    protected fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

}