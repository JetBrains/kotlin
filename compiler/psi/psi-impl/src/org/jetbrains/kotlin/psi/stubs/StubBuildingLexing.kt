/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclarationWithBody

/**
 * Lexer-based checks which allow the stub builder to avoid parsing lazily parseable code blocks.
 *
 * Each check is conservative: `false` is definitive, while `true` means that the code has to be parsed to get a precise answer.
 */
internal object StubBuildingLexing {
    /**
     * Keywords which start every declaration that can be stubbed inside a code block outside of scripts: local classes, interfaces,
     * objects (including object literals), and functions.
     * Anonymous functions are not stubbed, but the `fun` keyword alone doesn't tell them apart from local functions. It would take
     * a lookahead past the parentheses, as they may hold a receiver type (e.g., `fun (() -> Unit).foo()`), and anonymous functions
     * are too rare for such a fragile check to pay off.
     *
     * Other declarations inside a code block (e.g., local properties or type aliases) are never stubbed unless they are nested in
     * one of the declarations above.
     */
    private val STUBBED_DECLARATION_KEYWORDS: TokenSet = TokenSet.create(
        KtTokens.CLASS_KEYWORD,
        KtTokens.INTERFACE_KEYWORD,
        KtTokens.OBJECT_KEYWORD,
        KtTokens.FUN_KEYWORD,
    )

    private val SKIPPED_BEFORE_FIRST_STATEMENT: TokenSet = TokenSet.orSet(
        KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET,
        TokenSet.create(KtTokens.SEMICOLON),
    )

    /**
     * Returns whether the code [text] may contain declarations which have to be stubbed.
     *
     * The `class` keyword of class literals (e.g., `Int::class`) doesn't start a declaration, so it is ignored.
     */
    fun mayContainStubbedDeclarations(text: CharSequence): Boolean {
        val lexer = KotlinLexer()
        lexer.start(text)
        var previousTokenType: IElementType? = null
        while (true) {
            val tokenType = lexer.tokenType ?: return false
            if (tokenType in STUBBED_DECLARATION_KEYWORDS) {
                val isClassLiteral = tokenType == KtTokens.CLASS_KEYWORD && previousTokenType == KtTokens.COLONCOLON
                if (!isClassLiteral) return true
            }

            if (tokenType !in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET) {
                previousTokenType = tokenType
            }

            lexer.advance()
        }
    }

    /**
     * A lexer-based precondition for [org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck] which doesn't require parsing
     * the body block.
     *
     * Returns `true` if the first statement of the body block might be a (possibly parenthesized, labeled, or annotated) `contract { ... }`
     * or `kotlin.contracts.contract { ... }` call.
     */
    fun KtDeclarationWithBody.mayHaveLegacyContract(): Boolean {
        val bodyBlock = bodyBlockExpression ?: return false

        // The PSI check doesn't trigger parsing in this case
        val bodyNode = bodyBlock.node
        if (bodyNode !is LazyParseableElement || bodyNode.isParsed) return true

        val lexer = KotlinLexer()
        lexer.start(bodyNode.chars)
        if (lexer.tokenType != KtTokens.LBRACE) return true
        lexer.advance()

        while (lexer.tokenType in SKIPPED_BEFORE_FIRST_STATEMENT) {
            lexer.advance()
        }

        return when (lexer.tokenType) {
            // Parenthesized expressions or annotations
            KtTokens.LPAR, KtTokens.AT -> true

            KtTokens.IDENTIFIER -> {
                val identifier = lexer.tokenText
                if (identifier == "contract" || identifier == "kotlin") return true

                // A labeled expression
                lexer.advance()
                lexer.tokenType == KtTokens.AT
            }

            else -> false
        }
    }
}
