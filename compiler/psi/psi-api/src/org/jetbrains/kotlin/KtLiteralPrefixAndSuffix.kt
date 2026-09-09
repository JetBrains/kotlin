/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail

/**
 * The rule that a literal may not be glued to the token next to it, as in `a foo"bar"` or `1in a`.
 *
 * The rule is checked once per tree shape: `FirPrefixAndSuffixSyntaxChecker` walks the PSI, while for the light tree
 * the check rides along with the walk `KotlinLightParser` already makes over the freshly parsed tree. Only the walk
 * differs between the two, so what a literal is and which neighbours are prohibited is kept here.
 */
@KtImplementationDetail
object KtLiteralPrefixAndSuffix {
    /** The literal expressions the rule applies to. */
    val relevantLiteralTypes: TokenSet = TokenSet.create(
        KtNodeTypes.STRING_TEMPLATE,
        KtNodeTypes.CHARACTER_CONSTANT,
        KtNodeTypes.FLOAT_CONSTANT,
        KtNodeTypes.INTEGER_CONSTANT,
    )

    private val prohibitedAffixTypes = TokenSet.create(
        KtTokens.IDENTIFIER,
        KtTokens.INTEGER_LITERAL,
        KtTokens.FLOAT_LITERAL,
    )

    /**
     * Whether a token of type [tokenType] may not directly touch a literal. Any other token, whitespace and comments
     * included, separates the two.
     */
    fun isProhibitedPrefixOrSuffix(tokenType: IElementType): Boolean =
        tokenType in prohibitedAffixTypes || tokenType is KtKeywordToken
}
