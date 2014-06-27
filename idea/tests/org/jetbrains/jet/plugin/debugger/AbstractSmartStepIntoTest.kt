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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.debugger.actions.MethodSmartStepTarget
import org.jetbrains.jet.InTextDirectivesUtils
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiFormatUtilBase
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor

abstract class AbstractSmartStepIntoTest : JetLightCodeInsightFixtureTestCase() {
    private val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest(path: String) {
        fixture.configureByFile(path)

        val offset = fixture.getCaretOffset()
        val line = fixture.getDocument(fixture.getFile()!!)!!.getLineNumber(offset)

        val position = MockSourcePosition(_file = fixture.getFile(), _line = line, _offset = offset, _editor = fixture.getEditor())

        val actual = KotlinSmartStepIntoHandler().findSmartStepTargets(position).map { renderTarget(it) }

        val expected = InTextDirectivesUtils.findListWithPrefixes(fixture.getFile()?.getText()!!.replace("\\,", "+++"), "// EXISTS: ").map { it.replace("+++", ",") }

        for (actualTargetName in actual) {
            assert(expected.contains(actualTargetName), "Unexpected step into target was found: ${actualTargetName}\n${renderTableWithResults(expected, actual)}")
        }

        for (expectedTargetName in expected) {
            assert(actual.contains(expectedTargetName), "Missed step into target: ${expectedTargetName}\n${renderTableWithResults(expected, actual)}")
        }
    }

    private fun renderTableWithResults(expected: List<String>, actual: List<String>): String {
        val sb = StringBuilder()

        val maxExtStrSize = (expected.maxBy { it.size }?.size ?: 0) + 5
        val longerList = if (expected.size < actual.size) actual else expected
        val shorterList = if (expected.size < actual.size) expected else actual
        for ((i, element) in longerList.withIndices()) {
            sb.append(element)
            sb.append(" ".repeat(maxExtStrSize - element.size))
            if (i < shorterList.size) sb.append(shorterList[i])
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun renderTarget(target: SmartStepTarget): String {
        val sb = StringBuilder()

        val label = target.getLabel()
        if (label != null) {
            sb.append(label)
        }
        when (target) {
            is MethodSmartStepTarget -> {
                sb.append(PsiFormatUtil.formatMethod(
                        target.getMethod(),
                        PsiSubstitutor.EMPTY,
                        PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS,
                        PsiFormatUtilBase.SHOW_TYPE,
                        999
                ))
            }
            else -> {
                sb.append("Renderer for ${target.javaClass} should be implemented")
            }
        }
        return sb.toString()
    }

    override fun getTestDataPath(): String? {
        return PluginTestCaseBase.getTestDataPathBase() + "/debugger/smartStepInto"
    }

    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
