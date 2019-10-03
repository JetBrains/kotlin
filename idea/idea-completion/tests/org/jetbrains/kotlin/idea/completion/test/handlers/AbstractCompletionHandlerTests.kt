/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.test.rollbackCompilerOptions
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import java.io.File

abstract class AbstractCompletionHandlerTest(private val defaultCompletionType: CompletionType) : CompletionHandlerTestBase() {
    private val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private val LOOKUP_STRING_PREFIX = "ELEMENT:"
    private val ELEMENT_TEXT_PREFIX = "ELEMENT_TEXT:"
    private val TAIL_TEXT_PREFIX = "TAIL_TEXT:"
    private val COMPLETION_CHAR_PREFIX = "CHAR:"
    private val COMPLETION_CHARS_PREFIX = "CHARS:"
    private val CODE_STYLE_SETTING_PREFIX = "CODE_STYLE_SETTING:"

    protected open fun doTest(testPath: String) {
        setUpFixture(fileName())

        val tempSettings = CodeStyle.getSettings(project).clone()
        CodeStyle.setTemporarySettings(project, tempSettings)
        val fileText = FileUtil.loadFile(File(testPath))
        val configured = configureCompilerOptions(fileText, project, module)
        try {
            assertTrue("\"<caret>\" is missing in file \"$testPath\"", fileText.contains("<caret>"))


            val invocationCount = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX) ?: 1
            val lookupString = InTextDirectivesUtils.findStringWithPrefixes(fileText, LOOKUP_STRING_PREFIX)
            val itemText = InTextDirectivesUtils.findStringWithPrefixes(fileText, ELEMENT_TEXT_PREFIX)
            val tailText = InTextDirectivesUtils.findStringWithPrefixes(fileText, TAIL_TEXT_PREFIX)

            val completionChars = completionChars(
                char = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHAR_PREFIX),
                chars = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHARS_PREFIX)
            )

            val completionType = ExpectedCompletionUtils.getCompletionType(fileText) ?: defaultCompletionType

            val kotlinStyleSettings = KotlinCodeStyleSettings.getInstance(project)
            val commonStyleSettings = CodeStyle.getLanguageSettings(file)
            for (line in InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, CODE_STYLE_SETTING_PREFIX)) {
                val index = line.indexOfOrNull('=') ?: error("Invalid code style setting '$line': '=' expected")
                val settingName = line.substring(0, index).trim()
                val settingValue = line.substring(index + 1).trim()
                val (field, settings) = try {
                    kotlinStyleSettings::class.java.getField(settingName) to kotlinStyleSettings
                }
                catch (e: NoSuchFieldException) {
                    commonStyleSettings::class.java.getField(settingName) to commonStyleSettings
                }
                when (field.type.name) {
                    "boolean" -> field.setBoolean(settings, settingValue.toBoolean())
                    "int" -> field.setInt(settings, settingValue.toInt())
                    else -> error("Unsupported setting type: ${field.type}")
                }
            }

            doTestWithTextLoaded(
                completionType,
                invocationCount,
                lookupString,
                itemText,
                tailText,
                completionChars,
                File(testPath).name + ".after"
            )
        } finally {
            if (configured) {
                rollbackCompilerOptions(project, module)
            }
            CodeStyle.dropTemporarySettings(project)
            tearDownFixture()
        }
    }

    protected open fun setUpFixture(testPath: String) {
        fixture.configureWithExtraFile(testPath, ".dependency", ".dependency.1", ".dependency.2")
    }

    protected open fun tearDownFixture() {

    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

abstract class AbstractBasicCompletionHandlerTest : AbstractCompletionHandlerTest(CompletionType.BASIC)

abstract class AbstractSmartCompletionHandlerTest : AbstractCompletionHandlerTest(CompletionType.SMART)

abstract class AbstractCompletionCharFilterTest : AbstractCompletionHandlerTest(CompletionType.BASIC)

abstract class AbstractKeywordCompletionHandlerTest : AbstractCompletionHandlerTest(CompletionType.BASIC)
