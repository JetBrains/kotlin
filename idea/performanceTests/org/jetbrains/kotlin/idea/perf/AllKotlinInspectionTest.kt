/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInspection.*
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
            println("##teamcity[testStarted name='${it.tool.id}' captureStandardOutput='true']")
            val result = measureNanoTime {
                tool.analyze(psiFile)
            }
            results[it.tool.id] = result
            println("##teamcity[testFinished name='${it.tool.id}' duration='${(result * 1e-6).toLong()}']")
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
        if (file.textRange == null) return

        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
    }


    fun testWholeProjectPerformance() {

        val totals = mutableMapOf<String, Long>()

        tmp.walkTopDown().filter {
            it.extension == "kt" && "testData" !in it.path && "resources" !in it.path
        }.forEach {
            println("##teamcity[testSuiteStarted name='${it.relativeTo(tmp)}']")
            doTest(it).forEach { (k, v) -> totals.merge(k, v) { a, b -> a + b } }
            println("##teamcity[testSuiteFinished name='${it.relativeTo(tmp)}']")
        }

        totals.forEach { (k, v) ->
            println("##teamcity[testStarted name='total_$k' captureStandardOutput='true']")
            println("##teamcity[testFinished name='total_$k' duration='${(v * 1e-6).toLong()}']")
        }
    }

}