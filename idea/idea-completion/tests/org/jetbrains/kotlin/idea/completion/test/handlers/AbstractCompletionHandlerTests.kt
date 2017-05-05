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

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import java.io.File

abstract class AbstractCompletionHandlerTest(private val defaultCompletionType: CompletionType) : CompletionHandlerTestBase() {
    private val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private val LOOKUP_STRING_PREFIX = "ELEMENT:"
    private val ELEMENT_TEXT_PREFIX = "ELEMENT_TEXT:"
    private val TAIL_TEXT_PREFIX = "TAIL_TEXT:"
    private val COMPLETION_CHAR_PREFIX = "CHAR:"
    private val CODE_STYLE_SETTING_PREFIX = "CODE_STYLE_SETTING:"

    protected open fun doTest(testPath: String) {
        setUpFixture(testPath)

        val settingManager = CodeStyleSettingsManager.getInstance()
        val tempSettings = settingManager.currentSettings.clone()
        settingManager.setTemporarySettings(tempSettings)
        try {
            val fileText = FileUtil.loadFile(File(testPath))
            assertTrue("\"<caret>\" is missing in file \"$testPath\"", fileText.contains("<caret>"));
            val invocationCount = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX) ?: 1
            val lookupString = InTextDirectivesUtils.findStringWithPrefixes(fileText, LOOKUP_STRING_PREFIX)
            val itemText = InTextDirectivesUtils.findStringWithPrefixes(fileText, ELEMENT_TEXT_PREFIX)
            val tailText = InTextDirectivesUtils.findStringWithPrefixes(fileText, TAIL_TEXT_PREFIX)

            val completionCharString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHAR_PREFIX)
            val completionChar = when(completionCharString) {
                "\\n", null -> '\n'
                "\\t" -> '\t'
                else -> completionCharString.singleOrNull() ?: error("Incorrect completion char: \"$completionCharString\"")
            }

            val completionType = ExpectedCompletionUtils.getCompletionType(fileText) ?: defaultCompletionType

            val kotlinStyleSettings = KotlinCodeStyleSettings.getInstance(project)
            val commonStyleSettings = CodeStyleSettingsManager.getSettings(project).getCommonSettings(KotlinLanguage.INSTANCE)
            for (line in InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, CODE_STYLE_SETTING_PREFIX)) {
                val index = line.indexOfOrNull('=') ?: error("Invalid code style setting '$line': '=' expected")
                val settingName = line.substring(0, index).trim()
                val settingValue = line.substring(index + 1).trim()
                val (field, settings) = try {
                    kotlinStyleSettings::class.java.getDeclaredField(settingName) to kotlinStyleSettings
                }
                catch (e: NoSuchFieldException) {
                    commonStyleSettings::class.java.getDeclaredField(settingName) to commonStyleSettings
                }
                when (field.type.name) {
                    "boolean" -> field.setBoolean(settings, settingValue.toBoolean())
                    "int" -> field.setInt(settings, settingValue.toInt())
                    else -> error("Unsupported setting type: ${field.type}")
                }
            }

            doTestWithTextLoaded(completionType, invocationCount, lookupString, itemText, tailText, completionChar, testPath + ".after")
        }
        finally {
            settingManager.dropTemporarySettings()
            tearDownFixture()
        }
    }

    protected open fun setUpFixture(testPath: String) {
        fixture.configureByFile(testPath)
    }

    protected open fun tearDownFixture() {

    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

abstract class AbstractBasicCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)

abstract class AbstractSmartCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.SMART)

abstract class AbstractCompletionCharFilterTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)

abstract class AbstractKeywordCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)
