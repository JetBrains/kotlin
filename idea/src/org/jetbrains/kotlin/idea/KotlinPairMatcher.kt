/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.hint.DeclarationRangeUtil
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassBody

class KotlinPairMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(KtTokens.LPAR, KtTokens.RPAR, false),
        BracePair(KtTokens.LONG_TEMPLATE_ENTRY_START, KtTokens.LONG_TEMPLATE_ENTRY_END, false),
        BracePair(KtTokens.LBRACE, KtTokens.RBRACE, true),
        BracePair(KtTokens.LBRACKET, KtTokens.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return if (lbraceType == KtTokens.LONG_TEMPLATE_ENTRY_START) {
            // KotlinTypedHandler insert paired brace in this case
            false
        } else KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType === KtTokens.COLON
                || contextType === KtTokens.SEMICOLON
                || contextType === KtTokens.COMMA
                || contextType === KtTokens.RPAR
                || contextType === KtTokens.RBRACKET
                || contextType === KtTokens.RBRACE
                || contextType === KtTokens.LBRACE
                || contextType === KtTokens.LONG_TEMPLATE_ENTRY_END

    }

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        val element = file.findElementAt(openingBraceOffset)
        if (element == null || element is PsiFile) return openingBraceOffset
        val parent = element.parent
        return when (parent) {
            is KtClassBody, is KtBlockExpression ->
                DeclarationRangeUtil.getPossibleDeclarationAtRange(parent.parent)?.startOffset ?: openingBraceOffset

            else -> openingBraceOffset
        }
    }
}
