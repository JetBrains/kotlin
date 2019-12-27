/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import org.jetbrains.kotlin.idea.util.isMultiline
import org.jetbrains.kotlin.idea.util.needTrailingComma
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class TrailingCommaPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement =
        TrailingCommaVisitor(settings).process(source)

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange =
        TrailingCommaVisitor(settings).processText(source, rangeToReformat)
}

private class TrailingCommaVisitor(val settings: CodeStyleSettings) : KtTreeVisitorVoid() {
    private val myPostProcessor = PostFormatProcessorHelper(settings.kotlinCommonSettings)

    override fun visitParameterList(list: KtParameterList) = processCommaOwnerIfInRange(list) {
        super.visitParameterList(list)
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) = processCommaOwnerIfInRange(list) {
        super.visitValueArgumentList(list)
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) = processCommaOwnerIfInRange(expression.indicesNode) {
        super.visitArrayAccessExpression(expression)
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) = processCommaOwnerIfInRange(list) {
        super.visitTypeParameterList(list)
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) = processCommaOwnerIfInRange(typeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList)
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) = processCommaOwnerIfInRange(expression) {
        super.visitCollectionLiteralExpression(expression)
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) = processCommaOwnerIfInRange(jetWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
    }

    private fun processCommaOwnerIfInRange(element: KtElement, preHook: () -> Unit = {}) = processIfInRange(element) {
        preHook()
        processCommaOwner(element)
    }

    private fun processIfInRange(element: KtElement, block: () -> Unit = {}) {
        if (myPostProcessor.isElementPartlyInRange(element)) {
            block()
        }
    }

    private fun processCommaOwner(parent: KtElement) {
        val lastElement = parent.lastCommaOwnerOrComma ?: return
        val elementType = lastElement.safeAs<ASTNode>()?.elementType
        if (elementType === KtTokens.COMMA || parent.needComma) {
            // add a missing comma
            if (elementType !== KtTokens.COMMA) {
                changePsi(parent) { factory ->
                    lastElement.addCommaAfter(factory)
                    true
                }
            }

            correctCommaPosition(parent)
        }
    }

    private val KtElement.needComma: Boolean
        get() = when {
            this is KtWhenEntry -> needTrailingComma(settings)
            parent is KtFunctionLiteral -> parent.cast<KtFunctionLiteral>().needTrailingComma(settings)
            else -> settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA && isMultiline()
        }

    private fun changePsi(element: KtElement, update: (KtPsiFactory) -> Boolean) {
        val oldLength = element.textLength
        if (update(KtPsiFactory(element))) {
            val result = CodeStyleManager.getInstance(element.project).reformat(element)
            myPostProcessor.updateResultRange(oldLength, result.textLength)
        }
    }

    private fun correctCommaPosition(parent: KtElement) {
        val invalidElements = parent.firstChild?.siblings(withItself = false)?.mapNotNull {
            if (it.safeAs<ASTNode>()?.elementType != KtTokens.COMMA) return@mapNotNull null
            it.createSmartPointer()
        }?.toList() ?: return

        if (invalidElements.isNotEmpty()) {
            changePsi(parent) { factory ->
                var change = false
                for (pointerToComma in invalidElements) {
                    pointerToComma.element?.let {
                        if (correctComma(it, factory)) {
                            change = true
                        }
                    }
                }
                change
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
        rangeToReformat: TextRange
    ): TextRange {
        myPostProcessor.resultTextRange = rangeToReformat
        source.accept(this)
        return myPostProcessor.resultTextRange
    }

    companion object {
        private val LOG = Logger.getInstance(TrailingCommaVisitor::class.java)
    }
}

private fun PsiElement.addCommaAfter(factory: KtPsiFactory) {
    val comma = factory.createComma()
    parent.addAfter(comma, this)
}

private fun correctComma(comma: PsiElement, factory: KtPsiFactory): Boolean {
    val prevWithComment = comma.getPrevSiblingIgnoringWhitespace(false)
    val prevWithoutComment = comma.getPrevSiblingIgnoringWhitespaceAndComments(false)
    return when {
        prevWithoutComment?.equals(prevWithComment) == false -> {
            prevWithoutComment.addCommaAfter(factory)
            comma.delete()
            true
        }
        prevWithoutComment is KtParameter -> {
            val check = { element: PsiElement -> element is PsiWhiteSpace || element is PsiComment }
            val lastElement = prevWithoutComment.lastChild?.takeIf(check) ?: return false
            val firstElement = lastElement.siblings(forward = false, withItself = true).takeWhile(check).last()
            comma.parent.addRangeAfter(firstElement, lastElement, comma)
            prevWithoutComment.deleteChildRange(firstElement, lastElement)
            true
        }
        else ->
            false
    }
}

private val PsiElement.lastCommaOwnerOrComma: PsiElement?
    get() {
        val lastChild = lastSignificantChild ?: return null
        val withSelf = when (lastChild.safeAs<ASTNode>()?.elementType) {
            KtTokens.COMMA -> return lastChild
            KtTokens.RBRACKET, KtTokens.RPAR, KtTokens.RBRACE, KtTokens.GT, KtTokens.ARROW -> false
            else -> true
        }

        return lastChild.getPrevSiblingIgnoringWhitespaceAndComments(withSelf)
    }

private val PsiElement.lastSignificantChild: PsiElement?
    get() = when (this) {
        is KtWhenEntry -> arrow
        else -> lastChild
    }
