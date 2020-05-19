/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.formatter.KotlinLineIndentProvider
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Assert
import java.io.File

abstract class AbstractTypingIndentationTestBase : KotlinLightPlatformCodeInsightTestCase() {
    private val customLineIndentProvider: LineIndentProvider = KotlinLineIndentProvider()

    fun doNewlineTestWithInvert(afterInvFilePath: String) {
        doNewlineTest(afterInvFilePath, true)
    }

    @JvmOverloads
    fun doNewlineTest(afterFilePath: String, inverted: Boolean = false) {
        val testFileName = afterFilePath.substring(0, afterFilePath.indexOf("."))
        val testFileExtension = afterFilePath.substring(afterFilePath.lastIndexOf("."))
        val originFilePath = testFileName + testFileExtension
        val originalFileText = FileUtil.loadFile(File(originFilePath), true)
        val withoutCustomLineIndentProvider = InTextDirectivesUtils.findStringWithPrefixes(
            originalFileText,
            "// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER"
        ) != null

        try {
            val configurator = FormatSettingsUtil.createConfigurator(originalFileText, CodeStyle.getSettings(project))
            if (!inverted) {
                configurator.configureSettings()
            } else {
                configurator.configureInvertedSettings()
            }

            doNewlineTest(originFilePath, afterFilePath, withoutCustomLineIndentProvider)
        } finally {
            CodeStyle.getSettings(project).clearCodeStyleSettings()
        }
    }

    private fun doNewlineTest(beforeFilePath: String, afterFilePath: String, withoutCustomLineIndentProvider: Boolean) {
        KotlinLineIndentProvider.useFormatter = true
        typeAndCheck(beforeFilePath, afterFilePath, "with FormatterBasedLineIndentProvider")
        KotlinLineIndentProvider.useFormatter = false

        if (!withoutCustomLineIndentProvider) {
            typeAndCheck(beforeFilePath, afterFilePath, "with ${customLineIndentProvider.javaClass.simpleName}")
        }

        configureByFile(beforeFilePath)
        assertCustomIndentExist(withoutCustomLineIndentProvider)
    }

    private fun assertCustomIndentExist(withoutCustomLineIndentProvider: Boolean) {
        val offset = editor.caretModel.offset
        runWriteAction {
            editor.document.insertString(offset, "\n")
        }

        val customIndent = customLineIndentProvider.getLineIndent(project, editor, KotlinLanguage.INSTANCE, offset + 1)
        val condition = customIndent == null
        Assert.assertTrue(
            "${if (withoutCustomLineIndentProvider) "Remove" else "Add"} \"// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER\" or fix ${customLineIndentProvider.javaClass.simpleName}",
            if (withoutCustomLineIndentProvider) condition else !condition
        )
    }

    private fun typeAndCheck(beforeFilePath: String, afterFilePath: String, errorMessage: String) {
        configureByFile(beforeFilePath)
        executeAction(IdeActions.ACTION_EDITOR_ENTER)
        val actualTextWithCaret = StringBuilder(editor.document.text).insert(
            editor.caretModel.offset,
            EditorTestUtil.CARET_TAG
        ).toString()

        KotlinTestUtils.assertEqualsToFile(errorMessage, File(afterFilePath), actualTextWithCaret)
    }

    override fun getTestDataPath(): String = ""
}