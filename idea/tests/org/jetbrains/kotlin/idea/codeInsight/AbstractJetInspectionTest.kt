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
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public abstract class AbstractJetInspectionTest: LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = JetLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        val optionsFile = File(path)
        val options = FileUtil.loadFile(optionsFile, true)

        val inspectionClass = Class.forName(InTextDirectivesUtils.findStringWithPrefixes(options, "// INSPECTION_CLASS: ")!!)
        val toolWrapper = LocalInspectionToolWrapper(inspectionClass.newInstance() as LocalInspectionTool)

        val inspectionsTestDir = optionsFile.getParentFile()!!
        val srcDir = inspectionsTestDir.getParentFile()!!

        with(myFixture) {
            setTestDataPath("${JetTestUtils.getHomeDirectory()}/$srcDir")

            val psiFiles = srcDir
                    .listFiles { it.getName().endsWith(".kt") || it.getName().endsWith(".txt") || it.getName().endsWith(".xml") || it.getName().endsWith(".java") }!!
                    .map {
                        file ->
                        val text = FileUtil.loadFile(file, true)
                        val fileText =
                                if (text.startsWith("package") || !file.getName().endsWith(".kt"))
                                    text
                                else
                                    "package ${file.getName().trimTrailing(".kt")};$text"
                        configureByText(file.getName(), fileText)!!
                    }

            val isWithRuntime = psiFiles.any { InTextDirectivesUtils.findStringWithPrefixes(it.getText(), "// WITH_RUNTIME") != null }
            val fullJdk = psiFiles.any { InTextDirectivesUtils.findStringWithPrefixes(it.getText(), "// FULL_JDK") != null }

            try {
                if (isWithRuntime) {
                    ConfigLibraryUtil.configureKotlinRuntimeAndSdk(
                            myFixture.getModule(),
                            if (fullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
                    )
                }

                val scope = AnalysisScope(getProject(), psiFiles.map { it.getVirtualFile()!! })
                scope.invalidate()

                val inspectionManager = (InspectionManager.getInstance(getProject()) as InspectionManagerEx)
                val globalContext = CodeInsightTestFixtureImpl.createGlobalContextForTool(scope, getProject(), inspectionManager, toolWrapper)

                InspectionTestUtil.runTool(toolWrapper, scope, globalContext)
                InspectionTestUtil.compareToolResults(globalContext, toolWrapper, false, inspectionsTestDir.getPath())
            }
            finally {
                if (isWithRuntime) {
                    ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myFixture.getModule(), IdeaTestUtil.getMockJdk17())
                }
            }
        }
    }
}
