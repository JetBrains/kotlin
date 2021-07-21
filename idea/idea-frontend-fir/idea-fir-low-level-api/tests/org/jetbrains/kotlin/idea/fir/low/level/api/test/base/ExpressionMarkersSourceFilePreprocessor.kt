/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.util.parentOfType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        val withSelectedProcessed = processSelectedExpression(file, content)
        return processCaretExpression(file, withSelectedProcessed)
    }

    private fun processSelectedExpression(file: TestFile, content: String): String {
        val startCaretPosition = content.indexOfOrNull(TAGS.OPENING_EXPRESSION_TAG) ?: return content

        val endCaretPosition = content.indexOfOrNull(TAGS.CLOSING_EXPRESSION_TAG)
            ?: error("${TAGS.CLOSING_EXPRESSION_TAG} was not found in the file")

        check(startCaretPosition < endCaretPosition)
        testServices.expressionMarkerProvider.addSelectedExpression(
            file,
            TextRange.create(startCaretPosition, endCaretPosition - TAGS.OPENING_EXPRESSION_TAG.length)
        )
        return content
            .replace(TAGS.OPENING_EXPRESSION_TAG, "")
            .replace(TAGS.CLOSING_EXPRESSION_TAG, "")
    }

    private fun processCaretExpression(file: TestFile, content: String): String {
        val startCaretPosition = content.indexOfOrNull(TAGS.CARET) ?: return content

        testServices.expressionMarkerProvider.addCaret(file, startCaretPosition)
        return content
            .replace(TAGS.CARET, "")
    }

    object TAGS {
        const val OPENING_EXPRESSION_TAG = "<expr>"
        const val CLOSING_EXPRESSION_TAG = "</expr>"
        const val CARET = "<caret>"
    }
}

class ExpressionMarkerProvider(testServices: TestServices) : TestService {
    private val selected = mutableMapOf<String, TextRange>()

    @PrivateForInline
    val atCaret = mutableMapOf<String, Int>()

    fun addSelectedExpression(file: TestFile, range: TextRange) {
        selected[file.relativePath] = range
    }

    @OptIn(PrivateForInline::class)
    fun addCaret(file: TestFile, caret: Int) {
        atCaret[file.relativePath] = caret
    }

    @OptIn(PrivateForInline::class)
    fun getCaretPosition(file: KtFile): Int {
        return atCaret[file.name]
            ?: error("No caret found in file")
    }

    inline fun <reified P : KtElement> getElementOfTypAtCaret(file: KtFile): P {
        val offset = getCaretPosition(file)
        return file.findElementAt(offset)
            ?.parentOfType<P>()
            ?: error("No expression found at caret")
    }


    fun getSelectedElement(file: KtFile): KtElement {
        val range = selected[file.name]
            ?: error("No selected expression found in file")
        val elements = file.elementsInRange(range).trimWhitespaces()
        if (elements.size != 1) {
            error("Expected one element at rage but found ${elements.size} [${elements.joinToString { it::class.simpleName + ": " + it.text }}]")
        }
        return elements.single() as KtElement
    }

    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }
}

val TestServices.expressionMarkerProvider: ExpressionMarkerProvider by TestServices.testServiceAccessor()
