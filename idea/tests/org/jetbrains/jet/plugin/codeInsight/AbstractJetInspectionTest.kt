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
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

public abstract class AbstractJetInspectionTest: LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = JetLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        myFixture!!.setTestDataPath("${PluginTestCaseBase.getTestDataPathBase()}/codeInsight/inspections")
    }

    protected fun doTest(path: String) {
        val testDir = File(path).getName()

        val options = FileUtil.loadFile(File(path, "options.test"), true)
        val inspectionClass = Class.forName(InTextDirectivesUtils.findStringWithPrefixes(options, "// INSPECTION_CLASS: ")!!)

        myFixture!!.testInspection(testDir, LocalInspectionToolWrapper(inspectionClass.newInstance() as LocalInspectionTool))
    }
}
