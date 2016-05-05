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

import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementDownAction
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementUpAction
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.formatter.FormatSettingsUtil
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinDeclarationMover
import org.jetbrains.kotlin.idea.codeInsight.upDownMover.KotlinExpressionMover
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractCodeMoverTest : LightCodeInsightTestCase() {
    protected fun doTestClassBodyDeclaration(path: String) {
        doTest(path, KotlinDeclarationMover::class.java)
    }

    protected fun doTestExpression(path: String) {
        doTest(path, KotlinExpressionMover::class.java)
    }

    private fun doTest(path: String, moverClass: Class<out StatementUpDownMover>) {
        configureByFile(path)

        val fileText = FileUtil.loadFile(File(path), true)
        val direction = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MOVE: ")

        var down = true
        if ("up" == direction) {
            down = false
        }
        else if ("down" == direction) {
            down = true
        }
        else {
            fail("Direction is not specified")
        }

        val isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ")
        val isApplicableExpected = isApplicableString == null || isApplicableString == "true"

        val movers = Extensions.getExtensions(StatementUpDownMover.STATEMENT_UP_DOWN_MOVER_EP)
        val info = StatementUpDownMover.MoveInfo()
        var actualMover: StatementUpDownMover? = null
        for (mover in movers) {
            if (mover.checkAvailable(LightPlatformCodeInsightTestCase.getEditor(), LightPlatformCodeInsightTestCase.getFile(), info, down)) {
                actualMover = mover
                break
            }
        }

        assertTrue("No mover found", actualMover != null)
        assertEquals("Unmatched movers", moverClass.name, actualMover!!.javaClass.name)
        assertEquals("Invalid applicability", isApplicableExpected, info.toMove2 != null)

        if (isApplicableExpected) {
            invokeAndCheck(fileText, path, down)
        }
    }

    private fun invokeAndCheck(fileText: String, path: String, down: Boolean) {
        val codeStyleSettings = FormatSettingsUtil.getSettings()
        val configurator = FormatSettingsUtil.createConfigurator(fileText, codeStyleSettings)
        configurator.configureSettings()

        try {
            ApplicationManager.getApplication().runWriteAction {
                val action = if (down) MoveStatementDownAction() else MoveStatementUpAction()
                action.actionPerformed(LightPlatformCodeInsightTestCase.getEditor(), LightPlatformCodeInsightTestCase.getCurrentEditorDataContext())
            }

            val afterFilePath = path + ".after"
            try {
                checkResultByFile(afterFilePath)
            }
            catch (e: ComparisonFailure) {
                KotlinTestUtils.assertEqualsToFile(File(afterFilePath), LightPlatformCodeInsightTestCase.getEditor())
            }

        }
        finally {
            codeStyleSettings.clearCodeStyleSettings()
        }
    }

    override fun getTestDataPath() = ""
}
