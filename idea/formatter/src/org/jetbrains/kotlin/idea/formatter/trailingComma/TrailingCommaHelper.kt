/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.trailingComma

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.cast

object TrailingCommaHelper {
    fun findInvalidCommas(commaOwner: KtElement): List<PsiElement> = commaOwner.firstChild
        ?.siblings(withItself = false)
        ?.filter { it.isComma }
        ?.filter {
            it.prevLeaf(true)?.isLineBreak() == true || it.leafIgnoringWhitespace(false) != it.leafIgnoringWhitespaceAndComments(false)
        }?.toList().orEmpty()

    fun needComma(
        commaOwner: KtElement,
        settings: CodeStyleSettings?,
        checkExistingTrailingComma: Boolean = true,
    ): Boolean = when {
        commaOwner is KtWhenEntry ->
            commaOwner.needTrailingComma(settings, checkExistingTrailingComma)

        commaOwner.parent is KtFunctionLiteral ->
            commaOwner.parent.cast<KtFunctionLiteral>().needTrailingComma(settings, checkExistingTrailingComma)

        commaOwner is KtDestructuringDeclaration ->
            commaOwner.needTrailingComma(settings, checkExistingTrailingComma)

        else -> (checkExistingTrailingComma &&
                trailingCommaOrLastElement(commaOwner)?.isComma == true ||
                settings?.kotlinCustomSettings?.addTrailingCommaIsAllowedFor(commaOwner) != false) &&
                commaOwner.isMultiline()
    }

    fun trailingCommaExistsOrCanExist(psiElement: PsiElement, settings: CodeStyleSettings): Boolean =
        TrailingCommaContext.create(psiElement).commaExistsOrMayExist(settings.kotlinCustomSettings)

    fun trailingCommaExists(commaOwner: KtElement): Boolean = when (commaOwner) {
        is KtFunctionLiteral -> commaOwner.valueParameterList?.trailingComma != null
        is KtWhenEntry -> commaOwner.trailingComma != null
        is KtDestructuringDeclaration -> commaOwner.trailingComma != null
        else -> trailingCommaOrLastElement(commaOwner)?.isComma == true
    }

    fun trailingCommaOrLastElement(commaOwner: KtElement): PsiElement? {
        val lastChild = commaOwner.lastSignificantChild ?: return null
        val withSelf = when (PsiUtil.getElementType(lastChild)) {
            KtTokens.COMMA -> return lastChild
            in RIGHT_BARRIERS -> false
            else -> true
        }

        return lastChild.getPrevSiblingIgnoringWhitespaceAndComments(withSelf)?.takeIf {
            PsiUtil.getElementType(it) !in LEFT_BARRIERS
        }?.takeIfIsNotError()
    }

    fun elementBeforeFirstElement(commaOwner: KtElement): PsiElement? = when (commaOwner) {
        is KtParameterList -> {
            val parent = commaOwner.parent
            if (parent is KtFunctionLiteral) parent.lBrace else commaOwner.leftParenthesis
        }
        is KtWhenEntry -> commaOwner.parent.cast<KtWhenExpression>().openBrace
        is KtDestructuringDeclaration -> commaOwner.lPar
        else -> commaOwner.firstChild?.takeIfIsNotError()
    }

    fun elementAfterLastElement(commaOwner: KtElement): PsiElement? = when (commaOwner) {
        is KtParameterList -> {
            val parent = commaOwner.parent
            if (parent is KtFunctionLiteral) parent.arrow else commaOwner.rightParenthesis
        }
        is KtWhenEntry -> commaOwner.arrow
        is KtDestructuringDeclaration -> commaOwner.rPar
        else -> commaOwner.lastChild?.takeIfIsNotError()
    }

    private fun PsiElement.takeIfIsNotError(): PsiElement? = takeIf { it !is PsiErrorElement }

    private val RIGHT_BARRIERS = TokenSet.create(KtTokens.RBRACKET, KtTokens.RPAR, KtTokens.RBRACE, KtTokens.GT, KtTokens.ARROW)
    private val LEFT_BARRIERS = TokenSet.create(KtTokens.LBRACKET, KtTokens.LPAR, KtTokens.LBRACE, KtTokens.LT)
    private val PsiElement.lastSignificantChild: PsiElement?
        get() = when (this) {
            is KtWhenEntry -> arrow
            is KtDestructuringDeclaration -> rPar
            else -> lastChild
        }
}

private fun KtFunctionLiteral.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) valueParameterList?.trailingComma else null },
        globalStartOffset = { valueParameterList?.startOffset },
        globalEndOffset = { arrow?.endOffset },
    )

private fun KtWhenEntry.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
        additionalCheck = { !isElse && parent.cast<KtWhenExpression>().leftParenthesis != null },
        globalEndOffset = { arrow?.endOffset },
    )

private fun KtDestructuringDeclaration.needTrailingComma(
    settings: CodeStyleSettings?,
    checkExistingTrailingComma: Boolean = true
): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
        globalStartOffset = { lPar?.startOffset },
        globalEndOffset = { rPar?.endOffset },
    )

private fun <T : PsiElement> T.needTrailingComma(
    settings: CodeStyleSettings?,
    trailingComma: T.() -> PsiElement?,
    additionalCheck: () -> Boolean = { true },
    globalStartOffset: T.() -> Int? = PsiElement::startOffset,
    globalEndOffset: T.() -> Int? = PsiElement::endOffset,
): Boolean {
    if (trailingComma() == null && settings?.kotlinCustomSettings?.addTrailingCommaIsAllowedFor(this) == false) return false
    if (!additionalCheck()) return false

    val startOffset = globalStartOffset() ?: return false
    val endOffset = globalEndOffset() ?: return false
    return containsLineBreakInThis(startOffset, endOffset)
}