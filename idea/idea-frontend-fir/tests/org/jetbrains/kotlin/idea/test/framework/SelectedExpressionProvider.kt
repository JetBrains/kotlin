/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test.framework

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange

object SelectedExpressionProvider {
    fun getFileWithSelectedExpressions(fileText: String, createKtFile: (text: String) -> KtFile): KtFileWithSelectedExpression {
        val fileTextWithoutTags = fileText.replace(TAGS.OPENING_EXPRESSION_TAG, "").replace(TAGS.CLOSING_EXPRESSION_TAG, "")
        val ktFile = createKtFile(fileTextWithoutTags)
        val selectedExpression = run {
            val startCaretPosition = fileText.indexOf(TAGS.OPENING_EXPRESSION_TAG)
            if (startCaretPosition < 0) {
                return KtFileWithSelectedExpression(ktFile, selectedExpression = null)
            }
            val endCaretPosition = fileText.indexOf(TAGS.CLOSING_EXPRESSION_TAG)
            if (endCaretPosition < 0) {
                error("${TAGS.CLOSING_EXPRESSION_TAG} was not found in the file")
            }
            val elements = ktFile.elementsInRange(TextRange(startCaretPosition, endCaretPosition - TAGS.OPENING_EXPRESSION_TAG.length))
            if (elements.size != 1) {
                error("Expected one element at rage but found ${elements.size} [${elements.joinToString { it.text }}]")
            }
            elements.single() as KtElement
        }
        return KtFileWithSelectedExpression(ktFile, selectedExpression)
    }

    data class KtFileWithSelectedExpression(val file: KtFile, val selectedExpression: KtElement?)

    object TAGS {
        const val OPENING_EXPRESSION_TAG = "<expr>"
        const val CLOSING_EXPRESSION_TAG = "</expr>"
    }
}