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

package org.jetbrains.kotlin

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.idea.completion.renderDataFlowValue
import org.jetbrains.kotlin.test.JetTestUtils
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public abstract class AbstractDataFlowValueRenderingTest: JetLightCodeInsightFixtureTestCase() {
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
        val expression = element.getStrictParentOfType<JetExpression>()!!
        val info = expression.analyze().getDataFlowInfo(expression)

        val allValues = (info.getCompleteTypeInfo().keySet() + info.getCompleteNullabilityInfo().keySet()).toSet()
        val actual = allValues.map { renderDataFlowValue(it) }.filterNotNull().sort().joinToString("\n")

        JetTestUtils.assertEqualsToFile(File(FileUtil.getNameWithoutExtension(fileName) + ".txt"), actual)
    }
}
