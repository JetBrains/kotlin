/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.jetbrains.kotlin.idea.inspections.gradle.GradleKotlinxCoroutinesDeprecationInspection
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.junit.Test
import java.io.File
import kotlin.reflect.KMutableProperty0

class GradleQuickFixTest : GradleImportingTestCase() {
    private lateinit var codeInsightTestFixture: CodeInsightTestFixture

    private fun getTestDataPath() =
        PluginTestCaseBase.getTestDataPathBase() + "/gradle/fixes/" + getTestName(true).substringBefore('_')

    override fun setUpFixtures() {
        myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).fixture
        codeInsightTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myTestFixture)
        codeInsightTestFixture.setUp()
    }

    override fun tearDownFixtures() {
        codeInsightTestFixture.tearDown()
        @Suppress("UNCHECKED_CAST")
        (this::codeInsightTestFixture as KMutableProperty0<CodeInsightTestFixture?>).set(null)
        myTestFixture = null
    }

    @Test
    fun testUpdateKotlinxCoroutines() {
        doGradleQuickFixTest(GradleKotlinxCoroutinesDeprecationInspection())
    }

    private fun doGradleQuickFixTest(localInspectionTool: LocalInspectionTool) {
        val buildGradleVFile = createProjectSubFile("build.gradle", File(getTestDataPath(), "build.gradle").readText())
        importProject()

        applyInspectionFixes(localInspectionTool, buildGradleVFile)

        runInEdtAndWait {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        checkResult(buildGradleVFile)
    }

    private fun applyInspectionFixes(tool: LocalInspectionTool, file: VirtualFile) {
        invokeTestRunnable {
            val presentation = runInspection(tool, myProject, listOf(file))

            WriteCommandAction.runWriteCommandAction(myProject) {
                val foundProblems = presentation.problemElements.values.mapNotNull { it as? ProblemDescriptorBase }
                for (problem in foundProblems) {
                    val fixes = problem.fixes
                    if (fixes != null) {
                        fixes[0].applyFix(myProject, problem)
                    }
                }
            }
        }
    }

    private fun checkResult(file: VirtualFile) {
        KotlinTestUtils.assertEqualsToFile(File(getTestDataPath(), "build.gradle.after"), LoadTextUtil.loadText(file).toString())
    }
}