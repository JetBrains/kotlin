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

package org.jetbrains.jet.completion.handlers

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings
import com.intellij.codeInsight.lookup.LookupElement
import java.io.File
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.junit.Assert
import com.intellij.openapi.application.Result
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import kotlin.properties.Delegates
import org.jetbrains.jet.InTextDirectivesUtils

public abstract class AbstractSmartCompletionHandlerTest() : CompletionHandlerTestBase() {
    private val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private val LOOKUP_STRING_PREFIX = "ELEMENT:"
    private val ELEMENT_TEXT_PREFIX = "ELEMENT_TEXT:"
    private val TAIL_TEXT_PREFIX = "TAIL_TEXT:"
    private val COMPLETION_CHAR_PREFIX = "CHAR:"

    override val completionType: CompletionType = CompletionType.SMART
    override val testDataRelativePath: String = "/completion/handlers/smart"

    protected fun doTest(testPath: String) {
        fixture.configureByFile(testPath)

        val fileText = fixture.getFile()!!.getText()
        val invocationCount = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX) ?: 1
        val lookupString = InTextDirectivesUtils.findStringWithPrefixes(fileText, LOOKUP_STRING_PREFIX)
        val itemText = InTextDirectivesUtils.findStringWithPrefixes(fileText, ELEMENT_TEXT_PREFIX)
        val tailText = InTextDirectivesUtils.findStringWithPrefixes(fileText, TAIL_TEXT_PREFIX)
        val completionCharString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHAR_PREFIX)
        val completionChar = when(completionCharString) {
            "\\n", null -> '\n'
            "\\t" -> '\t'
            else -> error("Uknown completion char")
        }
        doTestWithTextLoaded(invocationCount, lookupString, itemText, tailText, completionChar)
    }
}
