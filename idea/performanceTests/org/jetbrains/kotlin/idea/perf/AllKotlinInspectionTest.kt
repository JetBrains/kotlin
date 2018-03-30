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

        val inspectionProfilesDir = File(".idea/inspectionProfiles")
        rootProjectFile.resolve(inspectionProfilesDir).copyRecursively(
            tmp.resolve(inspectionProfilesDir).also { it.parentFile.mkdirs() }
        )

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

    fun doTest(file: File): Map<String, Long> {

        val results = mutableMapOf<String, Long>()
        val vFile = VfsUtil.findFileByIoFile(file, false) ?: return results

        val psi = PsiManager.getInstance(project)
        val psiFile = psi.findFile(vFile) ?: return results


        profileTools.forEach {
            val tool = it.tool.tool as? LocalInspectionTool ?: return@forEach
            if (it.tool.language != null && it.tool.language!! !in setOf("kotlin", "UAST")) return@forEach
            val result = measureNanoTime {
                try {
                    tool.analyze(psiFile)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            results[it.tool.id] = result
        }

        return results
    }

    private fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

    fun LocalInspectionTool.analyze(file: PsiFile) {

        file.containingDirectory

        if (file.textRange == null) return

        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
    }


    inline fun tcSuite(name: String, l: () -> Unit) {
        println("##teamcity[testSuiteStarted name='$name']")
        l()
        println("##teamcity[testSuiteFinished name='$name']")
    }

    inline fun tcTest(name: String, l: () -> Long) {
        println("##teamcity[testStarted name='$name' captureStandardOutput='true']")
        val result = l()
        println("##teamcity[testFinished name='$name' duration='$result']")
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
                    val resultPerInspection = doTest(it)
                    resultPerInspection.forEach { (k, v) ->
                        appendInspectionResult(filePath, k, v)
                    }
                    val result = (resultPerInspection.values.sum() * 1e-6).toLong()
                    result
                }
            }
        }

        statsOutput.flush()
        statsOutput.close()


        tcSuite("Total") {
            totals.forEach { (k, v) ->
                tcTest(k) {
                    (v * 1e-6).toLong()
                }
            }
        }

    }

}