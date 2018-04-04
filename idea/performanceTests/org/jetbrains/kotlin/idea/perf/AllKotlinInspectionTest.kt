/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.codeInspection.ex.Tools
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.measureNanoTime


class AllKotlinInspectionTest : DaemonAnalyzerTestCase() {


    companion object {
        private val rootProjectFile = File(".").absoluteFile
        private val statsFile = File("build/stats.csv").absoluteFile
    }

    private val tmp by lazy { createTempDirectory() }

    override fun setUpProject() {

        println("Copying project to $tmp")

        rootProjectFile.copyRecursively(tmp)
        tmp.resolve(".idea").deleteRecursively()

        (ApplicationManager.getApplication() as ApplicationEx).doNotSave()
        myProject = ProjectUtil.openOrImport(tmp.path, null, false)
    }


    lateinit var profileTools: List<Tools>


    override fun setUp() {

        super.setUp()

        val wrappers = InspectionToolRegistrar.getInstance().createTools().map { it.tool }

        enableInspectionTools(*wrappers.toTypedArray())

        profileTools = ProjectInspectionProfileManager.getInstance(project).currentProfile.getAllEnabledInspectionTools(project)
    }

    data class PerFileTestResult(val perInspection: Map<String, Long>, val totalNs: Long, val errors: List<Throwable>)

    class ExceptionWhileInspection(insepctionId: String, cause: Throwable) : RuntimeException(insepctionId, cause)

    protected fun doTest(file: File): PerFileTestResult {

        val results = mutableMapOf<String, Long>()
        var totalNs = 0L
        val vFile = VfsUtil.findFileByIoFile(file, false) ?: run {
            return PerFileTestResult(results, totalNs, listOf(AssertionError("VirtualFile not found for $file")))
        }

        val psi = PsiManager.getInstance(project)
        val psiFile = psi.findFile(vFile) ?: run {
            return PerFileTestResult(results, totalNs, listOf(AssertionError("PsiFile not found for $vFile")))
        }

        val errors = mutableListOf<Throwable>()

        for (it in profileTools) {
            val localInspectionTool = it.tool.tool as? LocalInspectionTool ?: continue
            if (it.tool.language !in setOf(null, "kotlin", "UAST")) continue
            val result = measureNanoTime {
                try {
                    localInspectionTool.analyze(psiFile)
                } catch (t: Throwable) {
                    val myEx = ExceptionWhileInspection(it.tool.id, t)
                    myEx.printStackTrace()
                    errors += myEx
                }
            }
            results[it.tool.id] = result
            totalNs += result
            if (errors.isNotEmpty()) break
        }

        return PerFileTestResult(results, totalNs, errors)
    }

    private fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

    fun LocalInspectionTool.analyze(file: PsiFile) {
        if (file.textRange == null) return

        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
    }


    inline fun tcSuite(name: String, block: () -> Unit) {
        println("##teamcity[testSuiteStarted name='$name']")
        block()
        println("##teamcity[testSuiteFinished name='$name']")
    }

    inline fun tcTest(name: String, block: () -> Pair<Long, List<Throwable>>) {
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
        } else {
            println("##teamcity[testFinished name='$name' duration='$time']")
        }
    }

    fun String.tcEscape(): String {
        return this
            .replace("|", "||")
            .replace("[", "|[")
            .replace("]", "|]")
            .replace("\r", "|r")
            .replace("\n", "|n")
            .replace("'", "|'")
    }


    fun testWholeProjectPerformance() {

        val totals = mutableMapOf<String, Long>()

        val statsOutput = statsFile.bufferedWriter()

        statsOutput.appendln("File, InspectionID, Time")

        fun appendInspectionResult(file: String, id: String, nanoTime: Long) {
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
                    result.perInspection.forEach { (k, v) ->
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

}