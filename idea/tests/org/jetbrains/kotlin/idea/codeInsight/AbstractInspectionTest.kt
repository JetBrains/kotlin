/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentKotlinGradleVersionInspection
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractInspectionTest : KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        val ENTRY_POINT_ANNOTATION = "test.anno.EntryPoint"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.add(ENTRY_POINT_ANNOTATION)
    }

    override fun tearDown() {
        EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.remove(ENTRY_POINT_ANNOTATION)
        super.tearDown()
    }

    protected open fun configExtra(psiFiles: List<PsiFile>, options: String) {

    }

    protected fun doTest(path: String) {
        val optionsFile = File(path)
        val options = FileUtil.loadFile(optionsFile, true)

        val inspectionClass = Class.forName(InTextDirectivesUtils.findStringWithPrefixes(options, "// INSPECTION_CLASS: ")!!)
        val toolWrapper = LocalInspectionToolWrapper(inspectionClass.newInstance() as LocalInspectionTool)

        val tool = toolWrapper.tool
        if (tool is DifferentKotlinGradleVersionInspection) {
            tool.testVersionMessage = "\$PLUGIN_VERSION"
        }

        val fixtureClasses = InTextDirectivesUtils.findListWithPrefixes(options, "// FIXTURE_CLASS: ")

        val inspectionsTestDir = optionsFile.parentFile!!
        val srcDir = inspectionsTestDir.parentFile!!

        with(myFixture) {
            testDataPath = "${KotlinTestUtils.getHomeDirectory()}/$srcDir"

            val afterFiles = srcDir.listFiles { it -> it.name == "inspectionData" }?.single()?.listFiles { it -> it.extension == "after" } ?: emptyArray()
            val psiFiles = srcDir.walkTopDown().onEnter { it.name != "inspectionData" }.mapNotNull { file ->
                when {
                    file.isDirectory -> null
                    file.extension == "kt" -> {
                        val text = FileUtil.loadFile(file, true)
                        val fileText =
                                if (text.startsWith("package"))
                                    text
                                else
                                    "package ${file.nameWithoutExtension};$text"
                        configureByText(file.name, fileText)!!
                    }
                    file.extension == "gradle" -> {
                        val text = FileUtil.loadFile(file, true)
                        val fileText = text.replace("\$PLUGIN_VERSION", bundledRuntimeVersion())
                        configureByText(file.name, fileText)!!
                    }
                    else -> {
                        val filePath = file.relativeTo(srcDir).invariantSeparatorsPath
                        configureByFile(filePath)
                    }
                }
            }.toList()

            val isJs = srcDir.endsWith("js")

            val isWithRuntime = psiFiles.any { InTextDirectivesUtils.findStringWithPrefixes(it.text, "// WITH_RUNTIME") != null }
            val fullJdk = psiFiles.any { InTextDirectivesUtils.findStringWithPrefixes(it.text, "// FULL_JDK") != null }

            if (isJs) {
                assertFalse(isWithRuntime)
                assertFalse(fullJdk)
            }

            try {
                if (isJs) {
                    ConfigLibraryUtil.configureKotlinJsRuntime(myFixture.module)
                }
                if (isWithRuntime) {
                    ConfigLibraryUtil.configureKotlinRuntimeAndSdk(
                            myFixture.module,
                            if (fullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
                    )
                }

                fixtureClasses.forEach { TestFixtureExtension.loadFixture(it, myFixture.module) }

                configExtra(psiFiles, options)

                val scope = AnalysisScope(project, psiFiles.map { it.virtualFile!! })
                scope.invalidate()

                val inspectionManager = (InspectionManager.getInstance(project) as InspectionManagerEx)
                val globalContext = CodeInsightTestFixtureImpl.createGlobalContextForTool(scope, project, inspectionManager, toolWrapper)

                InspectionTestUtil.runTool(toolWrapper, scope, globalContext)
                InspectionTestUtil.compareToolResults(globalContext, toolWrapper, false, inspectionsTestDir.path)

                if (afterFiles.isNotEmpty()) {
                    globalContext.getPresentation(toolWrapper).problemDescriptors.forEach {
                        problem ->
                        problem.fixes?.forEach {
                            CommandProcessor.getInstance().executeCommand(project, {
                                runWriteAction { it.applyFix(project, problem) }
                            }, it.name, it.familyName)
                        }
                    }

                    for (filePath in afterFiles) {
                        val kotlinFile = psiFiles.first { filePath.name == it.name + ".after" }
                        KotlinTestUtils.assertEqualsToFile(filePath, kotlinFile.text)
                    }
                }

            }
            finally {
                fixtureClasses.forEach { TestFixtureExtension.unloadFixture(it) }

                if (isWithRuntime) {
                    ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myFixture.module, IdeaTestUtil.getMockJdk17())
                }
                if (isJs) {
                    ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(myFixture.module, IdeaTestUtil.getMockJdk17())
                }
            }
        }
    }
}
