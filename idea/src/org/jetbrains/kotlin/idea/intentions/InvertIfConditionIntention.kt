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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.idea.util.isUnit
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

public class InvertIfConditionIntention : JetSelfTargetingIntention<JetIfExpression>(javaClass(), "Invert 'if' condition") {
    override fun isApplicableTo(element: JetIfExpression, caretOffset: Int): Boolean {
        if (!element.getIfKeyword().getTextRange().containsOffset(caretOffset)) return false
        return element.getCondition() != null && element.getThen() != null
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val newCondition = negate(element.getCondition()!!)

        val newIf = handleSpecialCases(element, newCondition)
                    ?: handleStandardCase(element, newCondition)

        PsiDocumentManager.getInstance(newIf.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument())
        editor.moveCaret(newIf.getTextOffset())
    }

    private fun handleStandardCase(ifExpression: JetIfExpression, newCondition: JetExpression): JetIfExpression {
        val psiFactory = JetPsiFactory(ifExpression)

        val thenBranch = ifExpression.getThen()!!
        val elseBranch = ifExpression.getElse() ?: psiFactory.createEmptyBody()

        val newThen = if (elseBranch is JetIfExpression)
            psiFactory.wrapInABlock(elseBranch)
        else
            elseBranch

        val newElse = if (thenBranch is JetBlockExpression && thenBranch.getStatements().isEmpty())
            null
        else
            thenBranch

        return ifExpression.replaced(psiFactory.createIf(newCondition, newThen, newElse))
    }

    private fun handleSpecialCases(ifExpression: JetIfExpression, newCondition: JetExpression): JetIfExpression? {
        val elseBranch = ifExpression.getElse()
        if (elseBranch != null) return null

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
                        // build new then branch text from statements after if (we will add exit statement if necessary later)
                        var newIfBodyText = ifExpression.getContainingFile().getText().substring(first.startOffset, last.endOffset).trim()

                        // remove statements after if as they are moving under if
                        block.deleteChildRange(first, last)

                        if (lastThenStatement is JetReturnExpression && lastThenStatement.getReturnedExpression() == null) {
                            lastThenStatement.delete()
                        }
                        val updatedIf = copyThenBranchAfter(ifExpression)

                        // check if we need to add exit statement to then branch
                        if (exitStatementAfterIf != lastStatementInBlock) {
                            // don't insert the exit statement, if the new if statement placement has the same exit statement executed after it
                            val exitAfterNewIf = exitStatementExecutedAfter(updatedIf)
                            if (exitAfterNewIf == null || !exitAfterNewIf.matches(exitStatementAfterIf)) {
                                newIfBodyText += "\n" + exitStatementAfterIf.getText()
                            }
                        }

                        //TODO: no block if single?
                        val newIf = factory.createExpressionByPattern("if ($0) { $1 }", newCondition, newIfBodyText)
                        return updatedIf.replace(newIf) as JetIfExpression
                    }
                }
            }
        }


        val exitStatement = exitStatementExecutedAfter(ifExpression) ?: return null

        val updatedIf = copyThenBranchAfter(ifExpression)
        val newIf = factory.createExpressionByPattern("if ($0) $1", newCondition, exitStatement)
        return updatedIf.replace(newIf) as JetIfExpression
    }

    private fun copyThenBranchAfter(ifExpression: JetIfExpression): JetIfExpression {
        val factory = JetPsiFactory(ifExpression)
        val thenBranch = ifExpression.getThen() ?: return ifExpression

        val parent = ifExpression.getParent()
        if (parent !is JetBlockExpression) {
            assert(parent is JetContainerNode)
            val block = factory.createEmptyBody()
            block.addAfter(ifExpression, block.getLBrace())
            val newBlock = ifExpression.replaced(block)
            val newIf = newBlock.getStatements().single() as JetIfExpression
            return copyThenBranchAfter(newIf)
        }

        if (thenBranch is JetBlockExpression) {
            val range = thenBranch.contentRange()
            if (range != null) {
                parent.addRangeAfter(range.first, range.second, ifExpression)
                parent.addAfter(factory.createNewLine(), ifExpression)
            }
        }
        else {
            parent.addAfter(thenBranch, ifExpression)
            parent.addAfter(factory.createNewLine(), ifExpression)
        }
        return ifExpression
    }

    private fun JetBlockExpression.contentRange(): Pair<PsiElement, PsiElement>? {
        val first = getLBrace()?.siblings(withItself = false)?.firstOrNull { it !is PsiWhiteSpace } ?: return null
        val rBrace = getRBrace()
        if (first == rBrace) return null
        val last = rBrace!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
        return Pair(first, last)
    }

    private fun exitStatementExecutedAfter(expression: JetExpression): JetExpression? {
        val parent = expression.getParent()
        if (parent is JetBlockExpression) {
            val lastStatement = parent.getStatements().lastIsInstanceOrNull<JetExpression>()!!
            if (expression == lastStatement) {
                return exitStatementExecutedAfter(parent)
            }
            else {
                if (lastStatement.isExitStatement() && expression.siblings(withItself = false).firstIsInstance<JetExpression>() == lastStatement) {
                    return lastStatement
                }
                return null
            }
        }

        when (parent) {
            is JetNamedFunction -> {
                if (parent.getBodyExpression() == expression) {
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
                        if (expression == pparent.getBody()) {
                            return JetPsiFactory(expression).createExpression("continue")
                        }
                    }

                    is JetIfExpression -> {
                        if (expression == pparent.getThen() || expression == pparent.getElse()) {
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
