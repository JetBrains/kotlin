/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinSmartStepIntoHandler
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractSmartStepIntoTest : KotlinLightCodeInsightFixtureTestCase() {
    private val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest(path: String) {
        fixture.configureByFile(path)

        val offset = fixture.caretOffset
        val line = fixture.getDocument(fixture.file!!)!!.getLineNumber(offset)

        val lineStart = CodeInsightUtils.getStartLineOffset(file, line)!!
        val elementAtOffset = file.findElementAt(lineStart)

        val position = MockSourcePosition(_file = fixture.file,
                                          _line = line,
                                          _offset = offset,
                                          _editor = fixture.editor,
                                          _elementAt = elementAtOffset)

        val actual = KotlinSmartStepIntoHandler().findSmartStepTargets(position).map { it.presentation }

        val expected = InTextDirectivesUtils.findListWithPrefixes(fixture.file?.text!!.replace("\\,", "+++"), "// EXISTS: ").map { it.replace("+++", ",") }

        for (actualTargetName in actual) {
            assert(actualTargetName in expected) {
                "Unexpected step into target was found: $actualTargetName\n${renderTableWithResults(expected, actual)}" +
                "\n // EXISTS: ${actual.joinToString()}"
            }
        }

        for (expectedTargetName in expected) {
            assert(expectedTargetName in actual) {
                "Missed step into target: $expectedTargetName\n${renderTableWithResults(expected, actual)}" +
                "\n // EXISTS: ${actual.joinToString()}"
            }
        }
    }

    private fun renderTableWithResults(expected: List<String>, actual: List<String>): String {
        val sb = StringBuilder()

        val maxExtStrSize = (expected.maxBy { it.length }?.length ?: 0) + 5
        val longerList = (if (expected.size < actual.size) actual else expected).sorted()
        val shorterList = (if (expected.size < actual.size) expected else actual).sorted()
        for ((i, element) in longerList.withIndex()) {
            sb.append(element)
            sb.append(" ".repeat(maxExtStrSize - element.length))
            if (i < shorterList.size) sb.append(shorterList[i])
            sb.append("\n")
        }

        return sb.toString()
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/debugger/smartStepInto"
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
