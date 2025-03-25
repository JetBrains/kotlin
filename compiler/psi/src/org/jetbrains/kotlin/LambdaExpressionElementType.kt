/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.ElementTypeUtils.getKotlinBlockImbalanceCount
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.parsing.KotlinParser.Companion.parseLambdaExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class LambdaExpressionElementType : IErrorCounterReparseableElementType("LAMBDA_EXPRESSION", KotlinLanguage.INSTANCE) {
    override fun parseContents(chameleon: ASTNode): ASTNode? {
        val project = chameleon.getPsi().getProject()
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project, chameleon, null, KotlinLanguage.INSTANCE, chameleon.getChars()
        )
        return parseLambdaExpression(builder).getFirstChildNode()
    }

    override fun createNode(text: CharSequence?): ASTNode {
        return KtLambdaExpression(text)
    }

    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project): Boolean {
        return super.isParsable(parent, buffer, fileLanguage, project) && !wasArrowMovedOrDeleted(
            parent,
            buffer
        ) && !wasParameterCommaMovedOrDeleted(parent, buffer)
    }

    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language?, project: Project?): Int {
        return getKotlinBlockImbalanceCount(seq)
    }

    companion object {
        private fun wasArrowMovedOrDeleted(parent: ASTNode?, buffer: CharSequence): Boolean {
            val lambdaExpression: KtLambdaExpression? = findLambdaExpression(parent)
            if (lambdaExpression == null) {
                return false
            }

            val literal = lambdaExpression.getFunctionLiteral()
            val arrow = literal.getArrow()

            // No arrow in original node
            if (arrow == null) return false

            val arrowOffset = arrow.getStartOffsetInParent() + literal.getStartOffsetInParent()

            return hasTokenMoved(lambdaExpression.getText(), buffer, arrowOffset, KtTokens.ARROW)
        }

        private fun wasParameterCommaMovedOrDeleted(parent: ASTNode?, buffer: CharSequence): Boolean {
            val lambdaExpression: KtLambdaExpression? = findLambdaExpression(parent)
            if (lambdaExpression == null) {
                return false
            }

            val literal = lambdaExpression.getFunctionLiteral()
            val valueParameterList = literal.getValueParameterList()
            if (valueParameterList == null || valueParameterList.getParameters().size <= 1) {
                return false
            }

            val comma = valueParameterList.getFirstComma()
            if (comma == null) {
                return false
            }

            val commaOffset = comma.getTextOffset() - lambdaExpression.getTextOffset()
            return hasTokenMoved(lambdaExpression.getText(), buffer, commaOffset, KtTokens.COMMA)
        }

        private fun findLambdaExpression(parent: ASTNode?): KtLambdaExpression? {
            if (parent == null) return null

            val parentPsi = parent.getPsi()
            val lambdaExpressions = PsiTreeUtil.getChildrenOfType<KtLambdaExpression?>(parentPsi, KtLambdaExpression::class.java)
            if (lambdaExpressions == null || lambdaExpressions.size != 1) return null

            // Now works only when actual node can be spotted ambiguously. Need change in API.
            return lambdaExpressions[0]
        }

        private fun hasTokenMoved(oldText: String, buffer: CharSequence, oldOffset: Int, tokenType: IElementType?): Boolean {
            val oldLexer: Lexer = KotlinLexer()
            oldLexer.start(oldText)

            val newLexer: Lexer = KotlinLexer()
            newLexer.start(buffer)

            while (true) {
                val oldType = oldLexer.getTokenType()
                if (oldType == null) break // Didn't find an expected token. Consider it as no token was present.


                val newType = newLexer.getTokenType()
                if (newType == null) return true // New text was finished before reaching expected token in old text


                if (newType !== oldType) {
                    if (newType === KtTokens.WHITE_SPACE) {
                        newLexer.advance()
                        continue
                    } else if (oldType === KtTokens.WHITE_SPACE) {
                        oldLexer.advance()
                        continue
                    }

                    return true // Expected token was moved or deleted
                }

                if (oldType === tokenType && oldLexer.getCurrentPosition().getOffset() == oldOffset) {
                    break
                }

                oldLexer.advance()
                newLexer.advance()
            }

            return false
        }
    }
}
