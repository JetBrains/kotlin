/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.isUnit
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

public class InvertIfConditionIntention : JetSelfTargetingIntention<JetIfExpression>(javaClass(), "Invert 'if' condition") {
    override fun isApplicableTo(element: JetIfExpression, caretOffset: Int): Boolean {
        if (!element.getIfKeyword().getTextRange().containsOffset(caretOffset)) return false
        return element.getCondition() != null && element.getThen() != null
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val psiFactory = JetPsiFactory(element)

        val newCondition = negate(element.getCondition()!!)

        if (handleSpecialCases(element, newCondition)) return

        val thenBranch = element.getThen()!!
        val elseBranch = element.getElse() ?: psiFactory.createEmptyBody()

        val newThen = if (elseBranch is JetIfExpression)
            psiFactory.wrapInABlock(elseBranch)
        else
            elseBranch

        val newElse = if (thenBranch is JetBlockExpression && thenBranch.getStatements().isEmpty())
            null
        else
            thenBranch

        element.replace(psiFactory.createIf(newCondition, newThen, newElse))
    }

    private fun handleSpecialCases(ifExpression: JetIfExpression, newCondition: JetExpression): Boolean {
        val elseBranch = ifExpression.getElse()
        if (elseBranch != null) return false

        val factory = JetPsiFactory(ifExpression)

        val thenBranch = ifExpression.getThen()!!
        val lastThenStatement = thenBranch.lastBlockStatementOrThis()
        if (lastThenStatement.isExitStatement()) {
            val block = ifExpression.getParent() as? JetBlockExpression
            if (block != null) {
                val rBrace = block.getRBrace()
                val afterIfInBlock = ifExpression.siblings(withItself = false)
                        .takeWhile { it != rBrace }
                        .toList()
                val lastStatementInBlock = afterIfInBlock.lastIsInstanceOrNull<JetExpression>()
                if (lastStatementInBlock != null) {
                    val exitStatementAfterIf = if (lastStatementInBlock.isExitStatement())
                        lastStatementInBlock
                    else
                        exitStatementExecutedAfter(lastStatementInBlock)
                    if (exitStatementAfterIf != null) {
                        val first = afterIfInBlock.first()
                        val last = afterIfInBlock.last()
                        // build new then branch text from statements after if (will add exit statement if necessary later)
                        var newIfBodyText = ifExpression.getContainingFile().getText().substring(first.startOffset, last.endOffset).trim()

                        // remove statements after if as they are moving under if
                        block.deleteChildRange(first, last)

                        if (lastThenStatement is JetReturnExpression && lastThenStatement.getReturnedExpression() == null) {
                            lastThenStatement.delete()
                        }
                        copyThenBranchAfter(ifExpression)

                        // check if we need to add exit statement to then branch
                        if (exitStatementAfterIf != lastStatementInBlock) {
                            // don't insert the exit statement, if the new if statement placement has the same exit statement executed after it
                            val exitAfterNewIf = exitStatementExecutedAfter(ifExpression)
                            if (exitAfterNewIf == null || !exitAfterNewIf.matches(exitStatementAfterIf)) {
                                newIfBodyText += "\n" + exitStatementAfterIf.getText()
                            }
                        }

                        //TODO: no block if single?
                        ifExpression.replace(factory.createExpressionByPattern("if ($0) { $1 }", newCondition, newIfBodyText))
                        return true
                    }
                }
            }
        }


        val exitStatement = exitStatementExecutedAfter(ifExpression) ?: return false

        copyThenBranchAfter(ifExpression)
        ifExpression.replace(factory.createExpressionByPattern("if ($0) $1", newCondition, exitStatement))

        return true
    }

    private fun copyThenBranchAfter(ifExpression: JetIfExpression) {
        val factory = JetPsiFactory(ifExpression)
        val thenBranch = ifExpression.getThen() ?: return
        if (thenBranch is JetBlockExpression) {
            val range = thenBranch.contentRange()
            if (range != null) {
                ifExpression.getParent().addRangeAfter(range.first, range.second, ifExpression)
                ifExpression.getParent().addAfter(factory.createNewLine(), ifExpression)
            }
        }
        else {
            ifExpression.getParent().addAfter(thenBranch, ifExpression)
            ifExpression.getParent().addAfter(factory.createNewLine(), ifExpression)
        }
    }

    private fun JetBlockExpression.contentRange(): Pair<PsiElement, PsiElement>? {
        val first = getLBrace()?.siblings(withItself = false)?.firstOrNull { it !is PsiWhiteSpace } ?: return null
        val rBrace = getRBrace()
        if (first == rBrace) return null
        val last = rBrace!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
        return Pair(first, last)
    }

    private fun exitStatementExecutedAfter(expression: JetExpression): JetExpression? {
        val block = expression.getParent() as? JetBlockExpression ?: return null //TODO?

        val lastStatement = block.getStatements().lastIsInstanceOrNull<JetExpression>()!!
        if (expression != lastStatement) {
            if (lastStatement.isExitStatement() && expression.siblings(withItself = false).firstIsInstance<JetExpression>() == lastStatement) {
                return lastStatement
            }
            return null
        }

        val parent = block.getParent()
        when (parent) {
            is JetNamedFunction -> {
                if (parent.getBodyExpression() == block) {
                    if (!parent.hasBlockBody()) return null
                    val returnType = (parent.resolveToDescriptor() as FunctionDescriptor).getReturnType()
                    if (returnType == null || !returnType.isUnit()) return null
                    return JetPsiFactory(expression).createExpression("return")
                }
            }

            is JetContainerNode -> {
                val pparent = parent.getParent()
                when (pparent) {
                    is JetLoopExpression -> {
                        if (block == pparent.getBody()) {
                            return JetPsiFactory(expression).createExpression("continue")
                        }
                    }

                    is JetIfExpression -> {
                        if (block == pparent.getThen() || block == pparent.getElse()) {
                            return exitStatementExecutedAfter(pparent)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun JetExpression.isExitStatement(): Boolean {
        when (this) {
            is JetContinueExpression, is JetBreakExpression, is JetThrowExpression, is JetReturnExpression -> return true
            else -> return false
        }
    }

    companion object {
        private val NEGATABLE_OPERATORS = setOf(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ,
                JetTokens.EXCLEQEQEQ, JetTokens.IS_KEYWORD, JetTokens.NOT_IS, JetTokens.IN_KEYWORD,
                JetTokens.NOT_IN, JetTokens.LT, JetTokens.LTEQ, JetTokens.GT, JetTokens.GTEQ)

        private fun getNegatedOperatorText(token: IElementType): String {
            return when(token) {
                JetTokens.EQEQ -> JetTokens.EXCLEQ.getValue()
                JetTokens.EXCLEQ -> JetTokens.EQEQ.getValue()
                JetTokens.EQEQEQ -> JetTokens.EXCLEQEQEQ.getValue()
                JetTokens.EXCLEQEQEQ -> JetTokens.EQEQEQ.getValue()
                JetTokens.IS_KEYWORD -> JetTokens.NOT_IS.getValue()
                JetTokens.NOT_IS -> JetTokens.IS_KEYWORD.getValue()
                JetTokens.IN_KEYWORD -> JetTokens.NOT_IN.getValue()
                JetTokens.NOT_IN -> JetTokens.IN_KEYWORD.getValue()
                JetTokens.LT -> JetTokens.GTEQ.getValue()
                JetTokens.LTEQ -> JetTokens.GT.getValue()
                JetTokens.GT -> JetTokens.LTEQ.getValue()
                JetTokens.GTEQ -> JetTokens.LT.getValue()
                else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
            }
        }

        private fun negate(expression: JetExpression): JetExpression {
            val specialNegation = specialNegationText(expression)
            if (specialNegation != null) return specialNegation
            return JetPsiFactory(expression).createExpressionByPattern("!$0", expression)
        }

        private fun specialNegationText(expression: JetExpression): JetExpression? {
            val factory = JetPsiFactory(expression)
            when (expression) {
                is JetPrefixExpression -> {
                    if (expression.getOperationReference().getReferencedName() == "!") {
                        val baseExpression = expression.getBaseExpression()
                        if (baseExpression != null) {
                            return JetPsiUtil.safeDeparenthesize(baseExpression)
                        }
                    }
                }

                is JetBinaryExpression -> {
                    val operator = expression.getOperationToken()
                    if (operator !in NEGATABLE_OPERATORS) return null
                    val left = expression.getLeft() ?: return null
                    val right = expression.getRight() ?: return null
                    return factory.createExpressionByPattern("$0 $1 $2", left, getNegatedOperatorText(operator), right)
                }

                is JetConstantExpression -> {
                    return when (expression.getText()) {
                        "true" -> factory.createExpression("false")
                        "false" -> factory.createExpression("true")
                        else -> null
                    }
                }
            }
            return null
        }
    }
}
