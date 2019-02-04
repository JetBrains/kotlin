/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.testFramework.LightCodeInsightTestCase
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.IntroduceRefactoringException
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractExpressionSelectionTest : LightCodeInsightTestCase() {
    override fun getTestDataPath() = ""

    fun doTestExpressionSelection(path: String) {
        configureByFile(path)
        val expectedExpression = KotlinTestUtils.getLastCommentInFile(getFile() as KtFile)

        try {
            selectElement(
                    getEditor(),
                    getFile() as KtFile,
                    listOf(CodeInsightUtils.ElementKind.EXPRESSION)
            ) {
                assertNotNull("Selected expression mustn't be null", it)
                assertEquals(expectedExpression, it?.text)
            }
        }
        catch (e: IntroduceRefactoringException) {
            assertEquals(expectedExpression, "")
        }
    }
}
