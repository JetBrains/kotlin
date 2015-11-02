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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

public class InvertIfConditionIntention : SelfTargetingIntention<KtIfExpression>(javaClass(), "Invert 'if' condition") {
    override fun isApplicableTo(element: KtIfExpression, caretOffset: Int): Boolean {
        if (!element.getIfKeyword().getTextRange().containsOffset(caretOffset)) return false
        return element.getCondition() != null && element.getThen() != null
    }

    override fun applyTo(element: KtIfExpression, editor: Editor) {
        val newCondition = element.getCondition()!!.negate()

        val newIf = handleSpecialCases(element, newCondition)
                    ?: handleStandardCase(element, newCondition)

        val newIfCondition = newIf.condition
        val simplifyIntention = ConvertNegatedExpressionWithDemorgansLawIntention()
        if (newIfCondition is KtPrefixExpression && simplifyIntention.isApplicableTo(newIfCondition)) {
            simplifyIntention.applyTo(newIfCondition)
        }

        PsiDocumentManager.getInstance(newIf.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument())
        editor.moveCaret(newIf.getTextOffset())
    }

    private fun handleStandardCase(ifExpression: KtIfExpression, newCondition: KtExpression): KtIfExpression {
        val psiFactory = KtPsiFactory(ifExpression)

        val thenBranch = ifExpression.getThen()!!
        val elseBranch = ifExpression.getElse() ?: psiFactory.createEmptyBody()

        val newThen = if (elseBranch is KtIfExpression)
            psiFactory.createSingleStatementBlock(elseBranch)
        else
            elseBranch

        val newElse = if (thenBranch is KtBlockExpression && thenBranch.getStatements().isEmpty())
            null
        else
            thenBranch

        return ifExpression.replaced(psiFactory.createIf(newCondition, newThen, newElse))
    }

    private fun handleSpecialCases(ifExpression: KtIfExpression, newCondition: KtExpression): KtIfExpression? {
        val elseBranch = ifExpression.getElse()
        if (elseBranch != null) return null

        val factory = KtPsiFactory(ifExpression)

        val thenBranch = ifExpression.getThen()!!
        val lastThenStatement = thenBranch.lastBlockStatementOrThis()
        if (lastThenStatement.isExitStatement()) {
            val block = ifExpression.getParent() as? KtBlockExpression
            if (block != null) {
                val rBrace = block.getRBrace()
                val afterIfInBlock = ifExpression.siblings(withItself = false)
                        .takeWhile { it != rBrace }
                        .toList()
                val lastStatementInBlock = afterIfInBlock.lastIsInstanceOrNull<KtExpression>()
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

                        if (lastThenStatement is KtReturnExpression && lastThenStatement.getReturnedExpression() == null) {
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
                        return updatedIf.replace(newIf) as KtIfExpression
                    }
                }
            }
        }


        val exitStatement = exitStatementExecutedAfter(ifExpression) ?: return null

        val updatedIf = copyThenBranchAfter(ifExpression)
        val newIf = factory.createExpressionByPattern("if ($0) $1", newCondition, exitStatement)
        return updatedIf.replace(newIf) as KtIfExpression
    }

    private fun copyThenBranchAfter(ifExpression: KtIfExpression): KtIfExpression {
        val factory = KtPsiFactory(ifExpression)
        val thenBranch = ifExpression.getThen() ?: return ifExpression

        val parent = ifExpression.getParent()
        if (parent !is KtBlockExpression) {
            assert(parent is KtContainerNode)
            val block = factory.createEmptyBody()
            block.addAfter(ifExpression, block.getLBrace())
            val newBlock = ifExpression.replaced(block)
            val newIf = newBlock.getStatements().single() as KtIfExpression
            return copyThenBranchAfter(newIf)
        }

        if (thenBranch is KtBlockExpression) {
            val range = thenBranch.contentRange()
            if (!range.isEmpty) {
                parent.addRangeAfter(range.first, range.last, ifExpression)
                parent.addAfter(factory.createNewLine(), ifExpression)
            }
        }
        else {
            parent.addAfter(thenBranch, ifExpression)
            parent.addAfter(factory.createNewLine(), ifExpression)
        }
        return ifExpression
    }

    private fun exitStatementExecutedAfter(expression: KtExpression): KtExpression? {
        val parent = expression.getParent()
        if (parent is KtBlockExpression) {
            val lastStatement = parent.getStatements().last()
            if (expression == lastStatement) {
                return exitStatementExecutedAfter(parent)
            }
            else {
                if (lastStatement.isExitStatement() && expression.siblings(withItself = false).firstIsInstance<KtExpression>() == lastStatement) {
                    return lastStatement
                }
                return null
            }
        }

        when (parent) {
            is KtNamedFunction -> {
                if (parent.getBodyExpression() == expression) {
                    if (!parent.hasBlockBody()) return null
                    val returnType = (parent.resolveToDescriptor() as FunctionDescriptor).getReturnType()
                    if (returnType == null || !returnType.isUnit()) return null
                    return KtPsiFactory(expression).createExpression("return")
                }
            }

            is KtContainerNode -> {
                val pparent = parent.getParent()
                when (pparent) {
                    is KtLoopExpression -> {
                        if (expression == pparent.getBody()) {
                            return KtPsiFactory(expression).createExpression("continue")
                        }
                    }

                    is KtIfExpression -> {
                        if (expression == pparent.getThen() || expression == pparent.getElse()) {
                            return exitStatementExecutedAfter(pparent)
                        }
                    }
                }
            }
        }
        return null
    }
}
