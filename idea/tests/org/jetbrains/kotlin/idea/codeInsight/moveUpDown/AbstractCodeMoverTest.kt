/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.moveUpDown

import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementLeftAction
import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementRightAction
import com.intellij.codeInsight.editorActions.moveUpDown.LineMover
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementDownAction
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementUpAction
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.formatter.FormatSettingsUtil
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinDeclarationMover
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinExpressionMover
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.test.configureCodeStyleAndRun
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractMoveStatementTest : AbstractCodeMoverTest() {
    protected fun doTestClassBodyDeclaration(path: String) {
        doTest(path, KotlinDeclarationMover::class.java)
    }

    protected fun doTestExpression(path: String) {
        doTest(path, KotlinExpressionMover::class.java)
    }

    protected fun doTestExpressionWithTrailingComma(path: String) {
        doTest(path, KotlinExpressionMover::class.java, true)
    }

    protected fun doTestLine(path: String) {
        doTest(path, LineMover::class.java)
    }

    private fun doTest(path: String, defaultMoverClass: Class<out StatementUpDownMover>, trailingComma: Boolean = false) {
        doTest(path, trailingComma) { isApplicableExpected, direction ->
            val movers = Extensions.getExtensions(StatementUpDownMover.STATEMENT_UP_DOWN_MOVER_EP)
            val info = StatementUpDownMover.MoveInfo()
            val actualMover = movers.firstOrNull {
                it.checkAvailable(editor, file, info, direction == "down")
            } ?: error("No mover found")

            assertEquals("Unmatched movers", defaultMoverClass.name, actualMover::class.java.name)
            assertEquals("Invalid applicability", isApplicableExpected, info.toMove2 != null)
        }
    }
}

abstract class AbstractMoveLeftRightTest : AbstractCodeMoverTest() {
    protected fun doTest(path: String) {
        doTest(path) { _, _ -> }
    }
}

@Suppress("DEPRECATION")
abstract class AbstractCodeMoverTest : LightCodeInsightTestCase() {
    protected fun doTest(
        path: String,
        trailingComma: Boolean = false,
        isApplicableChecker: (isApplicableExpected: Boolean, direction: String) -> Unit
    ) {
        configureByFile(path)

        val fileText = FileUtil.loadFile(File(path), true)
        val direction = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MOVE: ") ?: error("No MOVE directive found")

        val action = when (direction) {
            "up" -> MoveStatementUpAction()
            "down" -> MoveStatementDownAction()
            "left" -> MoveElementLeftAction()
            "right" -> MoveElementRightAction()
            else -> error("Unknown direction: $direction")
        }

        val isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ")
        val isApplicableExpected = isApplicableString == null || isApplicableString == "true"

        isApplicableChecker(isApplicableExpected, direction)

        configureCodeStyleAndRun(
            project,
            {
                FormatSettingsUtil.createConfigurator(fileText, it).configureSettings()
                if (trailingComma) it.kotlinCustomSettings.ALLOW_TRAILING_COMMA = true
            }
        ) {
            invokeAndCheck(path, action, isApplicableExpected)
        }
    }

    private fun invokeAndCheck(path: String, action: EditorAction, isApplicableExpected: Boolean) {
        val editor = editor
        val dataContext = currentEditorDataContext

        val before = editor.document.text
        runWriteAction { action.actionPerformed(editor, dataContext) }

        val after = editor.document.text
        val actionDoesNothing = after == before

        TestCase.assertEquals(isApplicableExpected, !actionDoesNothing)

        if (isApplicableExpected) {
            val afterFilePath = "$path.after"
            try {
                checkResultByFile(afterFilePath)
            } catch (e: ComparisonFailure) {
                KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
            }
        }
    }

    override fun getTestDataPath() = ""
}
