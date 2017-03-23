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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.evaluate.addDebugExpressionIntoTmpFileForExtractFunction
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractBeforeExtractFunctionInsertionTest : LightCodeInsightTestCase() {

    fun doTest(path: String) {
        configureByFile(path)

        val offset = LightPlatformCodeInsightTestCase.getEditor().caretModel.offset
        val elementAt = KotlinCodeFragmentFactory.getContextElement(myFile.findElementAt(offset)) ?: throw AssertionError()
        val line = elementAt.getLineNumber()

        val fragmentFile = KotlinTestUtils.createFile("${myFile.name}fragment.kt", FileUtil.loadFile(File(path + ".fragment"), true), getProject())

        val imports = fragmentFile.importList?.text ?: ""
        val allText = fragmentFile.text
        val fragmentText = if (imports.isBlank()) allText else allText.substringAfter(imports, allText)

        val expressionList = addDebugExpressionIntoTmpFileForExtractFunction(
                myFile as KtFile,
                KtExpressionCodeFragment(getProject(), "fragment.kt", fragmentText, imports, elementAt),
                line)

        KotlinTestUtils.assertEqualsToFile(File(path + ".after"), expressionList.first().containingFile.text)
    }

    override fun getTestDataPath(): String {
        return ""
    }
}
