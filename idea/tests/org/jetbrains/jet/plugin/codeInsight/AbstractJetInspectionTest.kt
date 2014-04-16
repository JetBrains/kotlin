/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight

import java.io.File
import org.jetbrains.jet.InTextDirectivesUtils
import com.intellij.openapi.util.io.FileUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.jet.plugin.JetLightProjectDescriptor
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.testing.ConfigLibraryUtil
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import org.apache.commons.lang.SystemUtils;

public abstract class AbstractJetInspectionTest: LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = JetLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        val optionsFile = File(path)
        val options = FileUtil.loadFile(optionsFile, true)

        val inspectionClass = Class.forName(InTextDirectivesUtils.findStringWithPrefixes(options, "// INSPECTION_CLASS: ")!!)
        val toolWrapper = LocalInspectionToolWrapper(inspectionClass.newInstance() as LocalInspectionTool)

        val inspectionsTestDir = optionsFile.getParentFile()!!
        val srcDir = inspectionsTestDir.getParentFile()!!

        with(myFixture!!) {
            setTestDataPath("${JetTestCaseBuilder.getHomeDirectory()}/$srcDir")

            val virtualFiles = srcDir
                    .listFiles { it.getName().endsWith(".kt") }!!
                    .map { configureByFile(it.getName())!!.getVirtualFile()!! }

            val isWithRuntime = virtualFiles.any({ file ->
                InTextDirectivesUtils.findStringWithPrefixes(String(file.contentsToByteArray(), "utf-8"), "// WITH_RUNTIME") != null
             })

            try {

                if (isWithRuntime) {
                    ConfigLibraryUtil.configureKotlinRuntime(myFixture!!.getModule(), getFullJavaJDK());
                }

                val scope = AnalysisScope(getProject(), virtualFiles)
                scope.invalidate()

                val inspectionManager = (InspectionManager.getInstance(getProject()) as InspectionManagerEx)
                val globalContext = CodeInsightTestFixtureImpl.createGlobalContextForTool(scope, getProject(), inspectionManager, toolWrapper)

                InspectionTestUtil.runTool(toolWrapper, scope, globalContext, inspectionManager)
                InspectionTestUtil.compareToolResults(globalContext, toolWrapper, false, inspectionsTestDir.getPath())
            }
            finally {
                if (isWithRuntime) {
                    ConfigLibraryUtil.unConfigureKotlinRuntime(myFixture!!.getModule(), IdeaTestUtil.getMockJdk17());
                }
            }
        }
    }

    protected fun getFullJavaJDK(): Sdk {
        val javaPath = checkNotNull(SystemUtils.getJavaHome()?.getAbsolutePath(), "JDK Path must not be null")
        val jdk = JavaSdk.getInstance()?.createJdk("JDK", javaPath);
        return checkNotNull(jdk, "Could not obtain JDK instance")
    }
}
