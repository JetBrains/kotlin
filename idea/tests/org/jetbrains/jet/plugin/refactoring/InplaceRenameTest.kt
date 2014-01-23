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

package org.jetbrains.jet.plugin.refactoring

import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import kotlin.test.*
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.actionSystem.LangDataKeys

public class InplaceRenameTest : LightCodeInsightTestCase() {
    override fun isRunInWriteAction(): Boolean = false
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/refactoring/rename/inplace/"

    public fun testLocalVal() {
        doTestInplaceRename("y")
    }

    public fun testForLoop() {
        doTestInplaceRename("j")
    }

    public fun testTryCatch() {
        doTestInplaceRename("e1")
    }

    public fun testFunctionLiteral() {
        doTestInplaceRename("y")
    }

    public fun testFunctionLiteralParenthesis() {
        doTestInplaceRename("y")
    }

    public fun testLocalFunction() {
        doTestInplaceRename("bar")
    }

    public fun testFunctionParameterNotInplace() {
        doTestInplaceRename(null)
    }

    public fun testGlobalFunctionNotInplace() {
        doTestInplaceRename(null)
    }

    public fun testTopLevelValNotInplace() {
        doTestInplaceRename(null)
    }

    private fun doTestInplaceRename(newName: String?) {
        configureByFile(getTestName(false) + ".kt")
        val element = TargetElementUtilBase.findTargetElement(
                LightPlatformCodeInsightTestCase.myEditor,
                TargetElementUtilBase.ELEMENT_NAME_ACCEPTED or TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED
        )

        assertNotNull(element)

        val dataContext = SimpleDataContext.getSimpleContext(LangDataKeys.PSI_ELEMENT.getName(), element!!,
                                                             LightPlatformCodeInsightTestCase.getCurrentEditorDataContext())

        val handler = VariableInplaceRenameHandler()
        if (newName == null) {
            assertFalse(handler.isRenaming(dataContext), "In-place rename is allowed for " + element)
        }
        else {
            assertTrue(handler.isRenaming(dataContext), "In-place rename not allowed for " + element)
            CodeInsightTestUtil.doInlineRename(handler, newName, LightPlatformCodeInsightTestCase.getEditor(), element)
            checkResultByFile(getTestName(false) + ".kt.after")
        }
    }
}