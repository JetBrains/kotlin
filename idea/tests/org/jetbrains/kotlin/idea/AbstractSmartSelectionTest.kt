/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.testFramework.LightCodeInsightTestCase
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.getExpressionShortText
import org.jetbrains.kotlin.idea.refactoring.getSmartSelectSuggestions
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractSmartSelectionTest : LightCodeInsightTestCase() {
    fun doTestSmartSelection(path: String) {
        configureByFile(path)

        val expectedResultText = KotlinTestUtils.getLastCommentInFile(getFile() as KtFile)
        val elements = getSmartSelectSuggestions(getFile(), getEditor().caretModel.offset, CodeInsightUtils.ElementKind.EXPRESSION)
        assertEquals(expectedResultText, elements.joinToString(separator = "\n", transform = ::getExpressionShortText))
    }

    override fun getTestDataPath() = ""
}
