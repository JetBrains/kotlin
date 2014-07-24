/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.completion

import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.plugin.completion.renderDataFlowValue
import org.jetbrains.jet.JetTestUtils
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

abstract class AbstractDataFlowValueRenderingTest: JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase() + "/dataFlowValueRendering/"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return LightCodeInsightFixtureTestCase.JAVA_LATEST
    }

    fun doTest(fileName: String) {
        val fixture = myFixture
        fixture.configureByFile(fileName)

        val jetFile = fixture.getFile() as JetFile
        val element = jetFile.findElementAt(fixture.getCaretOffset())
        val expression = PsiTreeUtil.getParentOfType(element, javaClass<JetExpression>())!!
        val info = AnalyzerFacadeWithCache.getContextForElement(expression)[BindingContext.EXPRESSION_DATA_FLOW_INFO, expression]!!

        val allValues = (info.getCompleteTypeInfo().keySet() + info.getCompleteNullabilityInfo().keySet()).toSet()
        val actual = allValues.map { renderDataFlowValue(it) }.filterNotNull().sort().makeString("\n")

        JetTestUtils.assertEqualsToFile(File(FileUtil.getNameWithoutExtension(fileName) + ".txt"), actual)
    }
}