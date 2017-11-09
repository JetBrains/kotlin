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

package org.jetbrains.kotlin.idea.codeInsight.moveUpDown

import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementLeftAction
import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementRightAction
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementDownAction
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementUpAction
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.formatter.FormatSettingsUtil
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinDeclarationMover
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinExpressionMover
import org.jetbrains.kotlin.idea.core.script.isScriptDependenciesUpdaterDisabled
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

    private fun doTest(path: String, defaultMoverClass: Class<out StatementUpDownMover>) {
        doTest(path) { isApplicableExpected, direction ->
            val movers = Extensions.getExtensions(StatementUpDownMover.STATEMENT_UP_DOWN_MOVER_EP)
            val info = StatementUpDownMover.MoveInfo()
            val actualMover = movers.firstOrNull {
                it.checkAvailable(LightPlatformCodeInsightTestCase.getEditor(), LightPlatformCodeInsightTestCase.getFile(), info, direction == "down")
            } ?: error("No mover found")

            assertEquals("Unmatched movers", defaultMoverClass.name, actualMover::class.java.name)
            assertEquals("Invalid applicability", isApplicableExpected, info.toMove2 != null)
        }
    }
}

abstract class AbstractMoveLeftRightTest : AbstractCodeMoverTest() {
    protected fun doTest(path: String) {
        doTest(path) { _, _ ->  }
    }
}

abstract class AbstractCodeMoverTest : LightCodeInsightTestCase() {
    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = true
    }

    override fun tearDown() {
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = false
        super.tearDown()
    }

    protected fun doTest(path: String, isApplicableChecker: (isApplicableExpected: Boolean, direction: String) -> Unit) {
        configureByFile(path)

        val fileText = FileUtil.loadFile(File(path), true)
        val direction = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MOVE: ")
                        ?: error("No MOVE directive found")

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

        invokeAndCheck(fileText, path, action, isApplicableExpected)
    }

    private fun invokeAndCheck(fileText: String, path: String, action: EditorAction, isApplicableExpected: Boolean) {
        val codeStyleSettings = FormatSettingsUtil.getSettings()
        val configurator = FormatSettingsUtil.createConfigurator(fileText, codeStyleSettings)
        configurator.configureSettings()

        try {
            val editor = LightPlatformCodeInsightTestCase.getEditor()
            val dataContext = LightPlatformCodeInsightTestCase.getCurrentEditorDataContext()

            val before = editor.document.text
            runWriteAction { action.actionPerformed(editor, dataContext) }

            val after = editor.document.text
            val actionDoesNothing = after == before

            TestCase.assertEquals(isApplicableExpected, !actionDoesNothing)

            if (isApplicableExpected) {
                val afterFilePath = path + ".after"
                try {
                    checkResultByFile(afterFilePath)
                }
                catch (e: ComparisonFailure) {
                    KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
                }
            }
        }
        finally {
            codeStyleSettings.clearCodeStyleSettings()
        }
    }

    override fun getTestDataPath() = ""
}
