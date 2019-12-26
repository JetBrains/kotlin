/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import org.jetbrains.kotlin.idea.util.isMultiline
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.siblings
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

    private fun processCommaOwnerIfInRange(element: KtElement, preHook: () -> Unit = {}) {
        if (myPostProcessor.isElementPartlyInRange(element)) {
            preHook()
            processCommaOwner(element)
        }
    }

    private fun processCommaOwner(parent: KtElement) {
        val previous = parent.lastChild?.getPrevSiblingIgnoringWhitespaceAndComments() ?: return
        val elementType = previous.safeAs<ASTNode>()?.elementType
        if (elementType === KtTokens.COMMA || settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA && parent.isMultiline()) {
            // add a missing comma
            if (elementType !== KtTokens.COMMA) {
                changePsi(parent) { element, factory ->
                    element.addAfter(factory.createComma(), previous)
                }
            }

            correctCommaPosition(parent)
        }
    }

    private fun changePsi(element: KtElement, update: (KtElement, KtPsiFactory) -> Unit) {
        val oldLength = element.textLength
        update(element, KtPsiFactory(element))
        val result = CodeStyleManager.getInstance(element.project).reformat(element)
        myPostProcessor.updateResultRange(oldLength, result.textLength)
    }

    private fun correctCommaPosition(parent: KtElement) {
        val lPar = parent.children.firstOrNull() ?: return
        val rPar = parent.children.lastOrNull() ?: return
        val invalidElements = lPar.siblings(withItself = false).takeWhile { it != rPar }.mapNotNull {
            if (it !is ASTNode || it.elementType != KtTokens.COMMA) return@mapNotNull null

            val prevWithComment = it.getPrevSiblingIgnoringWhitespace(false)
            val prevWithoutComment = it.getPrevSiblingIgnoringWhitespaceAndComments(false)
            if (prevWithoutComment?.equals(prevWithComment) == false) {
                it.createSmartPointer() to prevWithoutComment.createSmartPointer()
            } else
                null
        }.toList()

        if (invalidElements.isNotEmpty()) {
            changePsi(parent) { element, factory ->
                for ((pointToComma, pointToElement) in invalidElements) {
                    element.addAfter(factory.createComma(), pointToElement.element)
                    pointToComma.element?.delete()
                }
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