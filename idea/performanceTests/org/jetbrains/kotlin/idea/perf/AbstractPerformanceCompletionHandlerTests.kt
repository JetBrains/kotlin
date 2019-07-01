/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.completion.test.handlers.CompletionHandlerTestBase
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.test.rollbackCompilerOptions
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import org.junit.AfterClass
import java.io.File

/**
 * inspired by @see AbstractCompletionHandlerTest
 */
abstract class AbstractPerformanceCompletionHandlerTests(
    private val defaultCompletionType: CompletionType,
    private val note: String = ""
) : CompletionHandlerTestBase() {

    private val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private val LOOKUP_STRING_PREFIX = "ELEMENT:"
    private val ELEMENT_TEXT_PREFIX = "ELEMENT_TEXT:"
    private val TAIL_TEXT_PREFIX = "TAIL_TEXT:"
    private val COMPLETION_CHAR_PREFIX = "CHAR:"
    private val CODE_STYLE_SETTING_PREFIX = "CODE_STYLE_SETTING:"

    companion object {
        @JvmStatic
        val statsMap: MutableMap<String, Stats> = mutableMapOf()

        @AfterClass
        @JvmStatic
        fun teardown() {
            statsMap.values.forEach { it.close() }
        }
    }

    private fun stats(): Stats {
        val suffix = "${defaultCompletionType.toString().toLowerCase()}${if (note.isNotEmpty()) "-$note" else ""}"
        return statsMap.computeIfAbsent(suffix) {
            Stats("completion-$suffix")
        }
    }

    override fun tearDown() {
        commitAllDocuments()
        super.tearDown()
    }

    protected open fun doPerfTest(testPath: String) {
        setUpFixture(testPath)

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

            val completionCharString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHAR_PREFIX)
            val completionChar = when(completionCharString) {
                "\\n", null -> '\n'
                "\\t" -> '\t'
                else -> completionCharString.singleOrNull() ?: error("Incorrect completion char: \"$completionCharString\"")
            }

            val completionType = ExpectedCompletionUtils.getCompletionType(fileText) ?: defaultCompletionType

            val kotlinStyleSettings = KotlinCodeStyleSettings.getInstance(project)
            val commonStyleSettings = CodeStyle.getLanguageSettings(file)
            for (line in InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, CODE_STYLE_SETTING_PREFIX)) {
                val index = line.indexOfOrNull('=') ?: error("Invalid code style setting '$line': '=' expected")
                val settingName = line.substring(0, index).trim()
                val settingValue = line.substring(index + 1).trim()
                val (field, settings) = try {
                    kotlinStyleSettings::class.java.getField(settingName) to kotlinStyleSettings
                } catch (e: NoSuchFieldException) {
                    commonStyleSettings::class.java.getField(settingName) to commonStyleSettings
                }
                when (field.type.name) {
                    "boolean" -> field.setBoolean(settings, settingValue.toBoolean())
                    "int" -> field.setInt(settings, settingValue.toInt())
                    else -> error("Unsupported setting type: ${field.type}")
                }
            }

            doPerfTestWithTextLoaded(
                testPath, completionType, invocationCount, lookupString, itemText, tailText, completionChar
            )
        } finally {
            if (configured) {
                rollbackCompilerOptions(project, module)
            }
            CodeStyle.dropTemporarySettings(project)
            tearDownFixture()
        }
    }

    private fun doPerfTestWithTextLoaded(
        testPath: String,
        completionType: CompletionType,
        time: Int,
        lookupString: String?,
        itemText: String?,
        tailText: String?,
        completionChar: Char
    ) {

        val testName = getTestName(false)

        val stats = stats()
        stats.perfTest(
            testName = testName,
            setUp = {
                setUpFixture(testPath)
            },
            test = {
                fixture.complete(completionType, time)

                if (lookupString != null || itemText != null || tailText != null) {
                    val item = getExistentLookupElement(lookupString, itemText, tailText)
                    if (item != null) {
                        selectItem(item, completionChar)
                    }
                }
            },
            tearDown = {
                assertNotNull(it)

                FileDocumentManager.getInstance().reloadFromDisk(editor.document)
                fixture.configureByText(KotlinFileType.INSTANCE, "")
                commitAllDocuments()
            })
    }

    protected open fun setUpFixture(testPath: String) {
        fixture.configureWithExtraFile(testPath, ".dependency", ".dependency.1", ".dependency.2")
    }

    protected open fun tearDownFixture() {

    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

abstract class AbstractPerformanceBasicCompletionHandlerTest : AbstractPerformanceCompletionHandlerTests(CompletionType.BASIC)

abstract class AbstractPerformanceSmartCompletionHandlerTest : AbstractPerformanceCompletionHandlerTests(CompletionType.SMART)

abstract class AbstractPerformanceCompletionCharFilterTest : AbstractPerformanceCompletionHandlerTests(
    CompletionType.BASIC,
    note = "charFilter"
)

abstract class AbstractPerformanceKeywordCompletionHandlerTest : AbstractPerformanceCompletionHandlerTests(
    CompletionType.BASIC,
    note = "keyword"
)
