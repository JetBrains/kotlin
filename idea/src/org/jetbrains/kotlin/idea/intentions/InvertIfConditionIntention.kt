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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

class InvertIfConditionIntention : SelfTargetingIntention<KtIfExpression>(KtIfExpression::class.java, "Invert 'if' condition") {
    override fun isApplicableTo(element: KtIfExpression, caretOffset: Int): Boolean {
        if (!element.ifKeyword.textRange.containsOffset(caretOffset)) return false
        return element.condition != null && element.then != null
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val rBrace = parentBlockRBrace(element)
        val commentSavingRange = if (rBrace != null)
            PsiChildRange(element, rBrace)
        else
            PsiChildRange.singleElement(element)
        val commentSaver = CommentSaver(commentSavingRange)

        val newCondition = element.condition!!.negate()

        val newIf = handleSpecialCases(element, newCondition)
                    ?: handleStandardCase(element, newCondition)

        val commentRestoreRange = if (rBrace != null)
            PsiChildRange(newIf, rBrace)
        else
            PsiChildRange(newIf, parentBlockRBrace(newIf) ?: newIf)
        commentSaver.restore(commentRestoreRange)

        val newIfCondition = newIf.condition
        val simplifyIntention = ConvertBinaryExpressionWithDemorgansLawIntention()
        (newIfCondition as? KtPrefixExpression)?.let {
            //use De Morgan's law only for negated condition to not make it more complex
            if (it.operationReference.getReferencedNameElementType() == KtTokens.EXCL) {
                val binaryExpr = (it.baseExpression as? KtParenthesizedExpression)?.expression as? KtBinaryExpression
                if (binaryExpr != null && simplifyIntention.isApplicableTo(binaryExpr)) {
                    simplifyIntention.applyTo(binaryExpr)
                }
            }
        }

        editor?.apply {
            unblockDocument()
            moveCaret(newIf.textOffset)
        }
    }

    private fun handleStandardCase(ifExpression: KtIfExpression, newCondition: KtExpression): KtIfExpression {
        val psiFactory = KtPsiFactory(ifExpression)

        val thenBranch = ifExpression.then!!
        val elseBranch = ifExpression.`else` ?: psiFactory.createEmptyBody()

        val newThen = if (elseBranch is KtIfExpression)
            psiFactory.createSingleStatementBlock(elseBranch)
        else
            elseBranch

        val newElse = if (thenBranch is KtBlockExpression && thenBranch.statements.isEmpty())
            null
        else
            thenBranch

        return ifExpression.replaced(psiFactory.createIf(newCondition, newThen, newElse))
    }

    private fun handleSpecialCases(ifExpression: KtIfExpression, newCondition: KtExpression): KtIfExpression? {
        val elseBranch = ifExpression.`else`
        if (elseBranch != null) return null

        val factory = KtPsiFactory(ifExpression)

        val thenBranch = ifExpression.then!!
        val lastThenStatement = thenBranch.lastBlockStatementOrThis()
        if (lastThenStatement.isExitStatement()) {
            val block = ifExpression.parent as? KtBlockExpression
            if (block != null) {
                val rBrace = block.rBrace
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
                        // build new then branch from statements after if (we will add exit statement if necessary later)
                        //TODO: no block if single?
                        val newThenRange = if (isEmptyReturn(lastThenStatement) && isEmptyReturn(lastStatementInBlock)) {
                            PsiChildRange(first, lastStatementInBlock.prevSibling).trimWhiteSpaces()
                        }
                        else {
                            PsiChildRange(first, last).trimWhiteSpaces()
                        }
                        val newIf = factory.createExpressionByPattern("if ($0) { $1 }", newCondition, newThenRange) as KtIfExpression

                        // remove statements after if as they are moving under if
                        block.deleteChildRange(first, last)

                        if (isEmptyReturn(lastThenStatement)) {
                            if (block.parent is KtDeclarationWithBody && block.parent !is KtFunctionLiteral) {
                                lastThenStatement.delete()
                            }
                        }
                        val updatedIf = copyThenBranchAfter(ifExpression)

                        // check if we need to add exit statement to then branch
                        if (exitStatementAfterIf != lastStatementInBlock) {
                            // don't insert the exit statement, if the new if statement placement has the same exit statement executed after it
                            val exitAfterNewIf = exitStatementExecutedAfter(updatedIf)
                            if (exitAfterNewIf == null || !exitAfterNewIf.matches(exitStatementAfterIf)) {
                                val newThen = newIf.then as KtBlockExpression
                                newThen.addBefore(exitStatementAfterIf, newThen.rBrace)
                            }
                        }

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

    private fun isEmptyReturn(statement: KtExpression) =
            statement is KtReturnExpression && statement.returnedExpression == null && statement.labeledExpression == null

    private fun copyThenBranchAfter(ifExpression: KtIfExpression): KtIfExpression {
        val factory = KtPsiFactory(ifExpression)
        val thenBranch = ifExpression.then ?: return ifExpression

        val parent = ifExpression.parent
        if (parent !is KtBlockExpression) {
            assert(parent is KtContainerNode)
            val block = factory.createEmptyBody()
            block.addAfter(ifExpression, block.lBrace)
            val newBlock = ifExpression.replaced(block)
            val newIf = newBlock.statements.single() as KtIfExpression
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
        val parent = expression.parent
        if (parent is KtBlockExpression) {
            val lastStatement = parent.statements.last()
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
                if (parent.bodyExpression == expression) {
                    if (!parent.hasBlockBody()) return null
                    val returnType = (parent.resolveToDescriptorIfAny() as? FunctionDescriptor)?.returnType
                    if (returnType == null || !returnType.isUnit()) return null
                    return KtPsiFactory(expression).createExpression("return")
                }
            }

            is KtContainerNode -> {
                val pparent = parent.parent
                when (pparent) {
                    is KtLoopExpression -> {
                        if (expression == pparent.body) {
                            return KtPsiFactory(expression).createExpression("continue")
                        }
                    }

                    is KtIfExpression -> {
                        if (expression == pparent.then || expression == pparent.`else`) {
                            return exitStatementExecutedAfter(pparent)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun parentBlockRBrace(element: KtIfExpression): PsiElement? {
        return (element.parent as? KtBlockExpression)?.rBrace
    }
}
