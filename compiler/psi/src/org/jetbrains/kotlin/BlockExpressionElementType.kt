/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.BLOCK_CODE_FRAGMENT
import org.jetbrains.kotlin.KtNodeTypes.FUNCTION_LITERAL
import org.jetbrains.kotlin.KtNodeTypes.SCRIPT
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.parsing.KotlinParser
import org.jetbrains.kotlin.psi.KtBlockExpression

class BlockExpressionElementType : IErrorCounterReparseableElementType("BLOCK", KotlinLanguage.INSTANCE), ICompositeElementType {

    override fun createCompositeNode() = KtBlockExpression(null)

    override fun createNode(text: CharSequence?) = KtBlockExpression(text)

    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project) =
        fileLanguage == KotlinLanguage.INSTANCE &&
                BlockExpressionElementType.isAllowedParentNode(parent) &&
                BlockExpressionElementType.isReparseableBlock(buffer) &&
                super.isParsable(buffer, fileLanguage, project)

    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language, project: Project) =
        ElementTypeUtils.getKotlinBlockImbalanceCount(seq)

    override fun parseContents(chameleon: ASTNode): ASTNode {
        val project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project, chameleon, null, KotlinLanguage.INSTANCE, chameleon.chars
        )

        return KotlinParser.parseBlockExpression(builder).firstChildNode
    }

    companion object {

        private fun isAllowedParentNode(node: ASTNode?) =
            node != null &&
                    SCRIPT != node.elementType &&
                    FUNCTION_LITERAL != node.elementType &&
                    BLOCK_CODE_FRAGMENT != node.elementType

        /**
         * Check if this text is block but not a lambda, please refer to parsing rules!
        @see [org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFunctionLiteral]
         */
        fun isReparseableBlock(blockText: CharSequence): Boolean {

            fun advanceWhitespacesCheckIsEndOrArrow(lexer: KotlinLexer): Boolean {
                lexer.advance()
                while (lexer.tokenType != null && lexer.tokenType != KtTokens.EOF) {
                    if (lexer.tokenType == KtTokens.ARROW) return true
                    if (lexer.tokenType != KtTokens.WHITE_SPACE) return false
                    lexer.advance()
                }
                return true
            }

            val lexer = KotlinLexer()
            lexer.start(blockText)

            // Try to parse a simple name list followed by an ARROW
            //   {a -> ...}
            //   {a, b -> ...}
            //   {(a, b) -> ... }
            if (lexer.tokenType != KtTokens.LBRACE) return false

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            if (lexer.tokenType != KtTokens.COLON &&
                lexer.tokenType != KtTokens.IDENTIFIER &&
                lexer.tokenType != KtTokens.LPAR
            ) return true

            val searchForRPAR = lexer.tokenType == KtTokens.LPAR

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            val preferParamsToExpressions = lexer.tokenType == KtTokens.COMMA || lexer.tokenType == KtTokens.COLON

            while (true) {

                if (lexer.tokenType == KtTokens.LBRACE) return true
                if (lexer.tokenType == KtTokens.RBRACE) return !preferParamsToExpressions

                if (searchForRPAR && lexer.tokenType == KtTokens.RPAR) {
                    return !advanceWhitespacesCheckIsEndOrArrow(lexer)
                }

                if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false
            }
        }
    }
}