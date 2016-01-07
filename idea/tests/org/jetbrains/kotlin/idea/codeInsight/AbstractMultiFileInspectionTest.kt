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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiFileInspectionTest : KotlinMultiFileTestCase() {
    init {
        myDoCompare = false
    }

    protected fun doTest(path: String) {
        val configFile = File(path)
        val config = JsonParser().parse(FileUtil.loadFile(configFile, true)) as JsonObject
        val inspection = LocalInspectionToolWrapper(Class.forName(config.getString("inspectionClass")).newInstance() as LocalInspectionTool)

        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        val withFullJdk = config["withFullJdk"]?.asBoolean ?: false

        doTest({ rootDir, rootAfter ->
                   try {
                       if (withRuntime) {
                           ConfigLibraryUtil.configureKotlinRuntimeAndSdk(
                                   module,
                                   if (withFullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
                           )
                       }

                       val scope = AnalysisScope(myProject)
                       scope.invalidate()

                       val globalContext = CodeInsightTestFixtureImpl.createGlobalContextForTool(
                               scope,
                               myProject,
                               InspectionManager.getInstance(myProject) as InspectionManagerEx,
                               inspection
                       )

                       InspectionTestUtil.runTool(inspection, scope, globalContext)
                       InspectionTestUtil.compareToolResults(globalContext, inspection, false, configFile.parent)
                   }
                   finally {
                       if (withRuntime) {
                           ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(module, IdeaTestUtil.getMockJdk17())
                       }
                   }
               },
               getTestDirName(true))
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.lastIndexOf('_')).replace('_', '/')
    }

    override fun getTestRoot() : String {
        return "/multiFileInspections/"
    }

    override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}
