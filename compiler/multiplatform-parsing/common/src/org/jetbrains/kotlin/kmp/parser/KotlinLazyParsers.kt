/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.LazyParser
import com.intellij.platform.syntax.LazyParsingContext
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.lexer.TokenList
import com.intellij.platform.syntax.parser.ProductionResult
import com.intellij.platform.syntax.parser.prepareProduction
import com.intellij.platform.syntax.tree.SyntaxNode
import com.intellij.platform.syntax.tree.findFirstChild
import com.intellij.platform.syntax.util.parser.SyntaxBuilderUtil
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.utils.KotlinParsing
import org.jetbrains.kotlin.kmp.parser.utils.SemanticWhitespaceAwareSyntaxBuilderImpl

// Element types from `KtNodeTypes` are accessed only inside functions because these objects are initialized during `KtNodeTypes` initialization.

/**
 * Parses and reparses collapsed [KtNodeTypes.BLOCK] nodes (bodies of functions, accessors, constructors, `init` blocks, `try` blocks, etc.).
 *
 * It mirrors `org.jetbrains.kotlin.BlockExpressionElementType` from the PSI parser.
 */
internal object KotlinBlockLazyParser : LazyParser {
    override fun parse(parsingContext: LazyParsingContext): ProductionResult {
        val builder = parsingContext.syntaxTreeBuilder
        KotlinParsing.createForTopLevel(SemanticWhitespaceAwareSyntaxBuilderImpl(builder)).parseBlockExpression()
        return prepareProduction(builder)
    }

    override fun canBeReparsedIncrementally(parsingContext: LazyParsingContext): Boolean {
        val tokens = parsingContext.tokenList
        return isAllowedParentNode(parsingContext.node.parent()) &&
                isReparseableBlock(tokens) &&
                SyntaxBuilderUtil.isBalancedBlock(tokens, KtTokens.LBRACE, KtTokens.RBRACE, parsingContext.cancellationProvider)
    }

    /**
     * Blocks inside these nodes don't have their own braces, and reparsing them as standalone blocks would change the tree structure.
     */
    private fun isAllowedParentNode(node: SyntaxNode?): Boolean {
        val type = node?.type ?: return false
        return type != KtNodeTypes.SCRIPT && type != KtNodeTypes.FUNCTION_LITERAL && type != KtNodeTypes.BLOCK_CODE_FRAGMENT
    }

    /**
     * Checks if the [tokens] represent a block but not a lambda.
     *
     * It should correspond to [org.jetbrains.kotlin.kmp.parser.utils.KotlinExpressionParsing.parseFunctionLiteral].
     */
    internal fun isReparseableBlock(tokens: TokenList): Boolean {
        var index = 0

        fun currentType(): SyntaxElementType? = tokens.getTokenType(index)

        fun advanceWhitespacesCheckIsEndOrArrow(): Boolean {
            index++
            while (true) {
                when (currentType()) {
                    null -> return true
                    KtTokens.ARROW -> return true
                    KtTokens.WHITE_SPACE -> index++
                    else -> return false
                }
            }
        }

        // Try to parse a simple name list followed by an ARROW
        //   {a -> ...}
        //   {a, b -> ...}
        //   {(a, b) -> ... }
        //   {[a, b] -> ... }
        if (currentType() != KtTokens.LBRACE) return false

        if (advanceWhitespacesCheckIsEndOrArrow()) return false

        val firstType = currentType()
        if (firstType != KtTokens.COLON &&
            firstType != KtTokens.IDENTIFIER &&
            firstType != KtTokens.LPAR &&
            firstType != KtTokens.LBRACKET
        ) return true

        val closingBracket = when (firstType) {
            KtTokens.LPAR -> KtTokens.RPAR
            KtTokens.LBRACKET -> KtTokens.RBRACKET
            else -> null
        }

        if (advanceWhitespacesCheckIsEndOrArrow()) return false

        val preferParamsToExpressions = currentType() == KtTokens.COMMA || currentType() == KtTokens.COLON

        while (true) {
            val type = currentType()

            if (type == KtTokens.LBRACE) return true
            if (type == KtTokens.RBRACE) return !preferParamsToExpressions

            if (closingBracket != null && type == closingBracket) {
                return !advanceWhitespacesCheckIsEndOrArrow()
            }

            if (advanceWhitespacesCheckIsEndOrArrow()) return false
        }
    }
}

/**
 * Parses and reparses collapsed [KtNodeTypes.LAMBDA_EXPRESSION] nodes.
 *
 * It mirrors `org.jetbrains.kotlin.LambdaExpressionElementType` from the PSI parser.
 */
internal object KotlinLambdaLazyParser : LazyParser {
    override fun parse(parsingContext: LazyParsingContext): ProductionResult {
        val builder = parsingContext.syntaxTreeBuilder
        KotlinParsing.createForTopLevel(SemanticWhitespaceAwareSyntaxBuilderImpl(builder)).parseLambdaExpression()
        return prepareProduction(builder)
    }

    override fun canBeReparsedIncrementally(parsingContext: LazyParsingContext): Boolean {
        val tokens = parsingContext.tokenList
        val oldLambda = parsingContext.node
        return SyntaxBuilderUtil.isBalancedBlock(tokens, KtTokens.LBRACE, KtTokens.RBRACE, parsingContext.cancellationProvider) &&
                !wasArrowMovedOrDeleted(oldLambda, tokens) &&
                !wasParameterCommaMovedOrDeleted(oldLambda, tokens)
    }

    /**
     * Arrow presence decides whether `{ ... }` in `if`, `when`, and loop bodies is a block or a lambda,
     * so if it's changed, the parent node should be reparsed.
     */
    private fun wasArrowMovedOrDeleted(oldLambda: SyntaxNode, newTokens: TokenList): Boolean {
        val functionLiteral = oldLambda.findFirstChild { it.type == KtNodeTypes.FUNCTION_LITERAL } ?: return false
        // No arrow in the original node
        val arrow = functionLiteral.findFirstChild { it.type == KtTokens.ARROW } ?: return false
        return hasTokenMoved(oldLambda, newTokens, arrow.startOffset - oldLambda.startOffset, KtTokens.ARROW)
    }

    /**
     * A comma after the first parameter affects whether parameters are preferred to expressions.
     */
    private fun wasParameterCommaMovedOrDeleted(oldLambda: SyntaxNode, newTokens: TokenList): Boolean {
        val functionLiteral = oldLambda.findFirstChild { it.type == KtNodeTypes.FUNCTION_LITERAL } ?: return false
        val parameterList = functionLiteral.findFirstChild { it.type == KtNodeTypes.VALUE_PARAMETER_LIST } ?: return false
        val comma = parameterList.findFirstChild { it.type == KtTokens.COMMA } ?: return false
        return hasTokenMoved(oldLambda, newTokens, comma.startOffset - oldLambda.startOffset, KtTokens.COMMA)
    }

    /**
     * Checks that the tokens of the old lambda text up to the [tokenType] at [oldOffset] match the [newTokens] ignoring whitespaces.
     */
    private fun hasTokenMoved(oldLambda: SyntaxNode, newTokens: TokenList, oldOffset: Int, tokenType: SyntaxElementType): Boolean {
        val oldLexer = KotlinLexer()
        oldLexer.start(oldLambda.text)
        var newIndex = 0

        while (true) {
            val oldType = oldLexer.getTokenType() ?: break // Didn't find an expected token. Consider it as no token was present.

            val newType = newTokens.getTokenType(newIndex) ?: return true // New text was finished before reaching expected token in old text

            if (newType != oldType) {
                if (newType == KtTokens.WHITE_SPACE) {
                    newIndex++
                    continue
                } else if (oldType == KtTokens.WHITE_SPACE) {
                    oldLexer.advance()
                    continue
                }

                return true // Expected token was moved or deleted
            }

            if (oldType == tokenType && oldLexer.getTokenStart() == oldOffset) {
                break
            }

            oldLexer.advance()
            newIndex++
        }

        return false
    }
}
