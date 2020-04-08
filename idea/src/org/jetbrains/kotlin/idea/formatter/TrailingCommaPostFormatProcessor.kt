/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import com.intellij.psi.util.PsiUtil.getElementType
import org.jetbrains.kotlin.idea.formatter.TrailingCommaHelper.findInvalidCommas
import org.jetbrains.kotlin.idea.formatter.TrailingCommaHelper.needComma
import org.jetbrains.kotlin.idea.formatter.TrailingCommaHelper.trailingCommaAllowedInModule
import org.jetbrains.kotlin.idea.formatter.TrailingCommaHelper.trailingCommaOrLastElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.siblings

class TrailingCommaPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement =
        TrailingCommaPostFormatVisitor(settings).process(source)

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange =
        TrailingCommaPostFormatVisitor(settings).processText(source, rangeToReformat)
}

private class TrailingCommaPostFormatVisitor(val settings: CodeStyleSettings) : TrailingCommaVisitor() {
    private val myPostProcessor = PostFormatProcessorHelper(settings.kotlinCommonSettings)

    override fun process(commaOwner: KtElement) = processIfInRange(commaOwner) {
        processCommaOwner(commaOwner)
    }

    private fun processIfInRange(element: KtElement, block: () -> Unit = {}) {
        if (myPostProcessor.isElementPartlyInRange(element)) {
            block()
        }
    }

    private fun processCommaOwner(parent: KtElement) {
        if (!postFormatIsEnable(parent)) return

        val lastElement = trailingCommaOrLastElement(parent) ?: return
        val elementType = getElementType(lastElement)
        updatePsi(parent) {
            when {
                needComma(parent, settings, false) -> {
                    // add a missing comma
                    if (elementType != KtTokens.COMMA && trailingCommaAllowedInModule(parent)) {
                        lastElement.addCommaAfter(KtPsiFactory(parent))
                    }

                    correctCommaPosition(parent)
                }
                needComma(parent, settings) -> {
                    correctCommaPosition(parent)
                }
                elementType == KtTokens.COMMA -> {
                    // remove redundant comma
                    lastElement.delete()
                }
            }
        }
    }

    private fun updatePsi(element: KtElement, block: () -> Unit) {
        element.putUserData(IS_INSIDE, true)
        val oldLength = element.parent.textLength
        block()

        val resultElement = CodeStyleManager.getInstance(element.project).reformat(element)
        myPostProcessor.updateResultRange(oldLength, resultElement.parent.textLength)
        element.putUserData(IS_INSIDE, false)
    }

    private fun correctCommaPosition(parent: KtElement) {
        for (pointerToComma in findInvalidCommas(parent).map { it.createSmartPointer() }) {
            pointerToComma.element?.let {
                correctComma(it)
            }
        }
    }

    fun process(formatted: PsiElement): PsiElement {
        LOG.assertTrue(formatted.isValid)
        formatted.accept(this)
        return formatted
    }

    fun processText(
        source: PsiFile,
        rangeToReformat: TextRange,
    ): TextRange {
        myPostProcessor.resultTextRange = rangeToReformat
        source.accept(this)
        return myPostProcessor.resultTextRange
    }

    companion object {
        private val LOG = Logger.getInstance(TrailingCommaVisitor::class.java)
        private val IS_INSIDE = Key.create<Boolean>("TrailingCommaPostFormat")
        private fun postFormatIsEnable(source: PsiElement): Boolean = source.getUserData(IS_INSIDE) != true
    }
}

private fun PsiElement.addCommaAfter(factory: KtPsiFactory) {
    val comma = factory.createComma()
    parent.addAfter(comma, this)
}

private fun correctComma(comma: PsiElement) {
    val prevWithComment = comma.leafIgnoringWhitespace(false) ?: return
    val prevWithoutComment = comma.leafIgnoringWhitespaceAndComments(false) ?: return
    if (prevWithComment != prevWithoutComment) {
        val check = { element: PsiElement -> element is PsiWhiteSpace || element is PsiComment }
        val firstElement = prevWithComment.siblings(forward = false, withItself = true).takeWhile(check).last()
        val commentOwner = prevWithComment.parent
        comma.parent.addRangeAfter(firstElement, prevWithComment, comma)
        commentOwner.deleteChildRange(firstElement, prevWithComment)
    }
}
