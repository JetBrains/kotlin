/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.evaluate.addDebugExpressionIntoTmpFileForExtractFunction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.concurrent.Callable

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

        val expressionList = ApplicationManager.getApplication().executeOnPooledThread(Callable<List<KtExpression>> {
            runReadAction {
                addDebugExpressionIntoTmpFileForExtractFunction(
                        myFile as KtFile,
                        KtExpressionCodeFragment(getProject(), "fragment.kt", fragmentText, imports, elementAt),
                        line)
            }
        }).get()

        KotlinTestUtils.assertEqualsToFile(File(path + ".after"), expressionList.first().containingFile.text)
    }

    override fun getTestDataPath(): String {
        return ""
    }
}
