/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtil.getElementType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings
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

    fun trailingCommaOrLastElement(commaOwner: KtElement): PsiElement? {
        val lastChild = commaOwner.lastSignificantChild ?: return null
        val withSelf = when (getElementType(lastChild)) {
            KtTokens.COMMA -> return lastChild
            in RIGHT_BARRIERS -> false
            else -> true
        }

        return lastChild.getPrevSiblingIgnoringWhitespaceAndComments(withSelf)?.takeIf {
            getElementType(it) !in LEFT_BARRIERS
        }?.takeIfIsNotError()
    }

    fun trailingCommaAllowedInModule(source: PsiElement): Boolean =
        Registry.`is`("kotlin.formatter.allowTrailingCommaInAnyProject", false) ||
                source.module?.languageVersionSettings?.supportsFeature(LanguageFeature.TrailingCommas) == true

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

fun PsiElement.leafIgnoringWhitespace(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace }

fun PsiElement.leafIgnoringWhitespaceAndComments(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace && it !is PsiComment }

fun PsiElement.leaf(forward: Boolean = true, filter: (PsiElement) -> Boolean): PsiElement? =
    if (forward) nextLeaf(filter)
    else prevLeaf(filter)

val PsiElement.isComma: Boolean get() = getElementType(this) == KtTokens.COMMA