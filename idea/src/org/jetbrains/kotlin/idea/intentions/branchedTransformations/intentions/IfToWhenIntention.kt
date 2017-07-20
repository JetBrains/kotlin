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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.getSubjectToIntroduce
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceSubject
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.siblings
import java.util.*

class IfToWhenIntention : SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' with 'when'") {
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        if (element.then == null) return null
        return element.ifKeyword.textRange
    }

    private fun canPassThrough(expression: KtExpression?): Boolean =
            when (expression) {
                is KtReturnExpression, is KtThrowExpression ->
                    false
                is KtBlockExpression ->
                    expression.statements.all { canPassThrough(it) }
                is KtIfExpression ->
                    canPassThrough(expression.then) || canPassThrough(expression.`else`)
                else ->
                    true
            }

    private fun buildNextBranch(ifExpression: KtIfExpression): KtExpression? {
        var nextSibling = ifExpression.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        return when (nextSibling) {
            is KtIfExpression ->
                if (nextSibling.then == null) null else nextSibling
            else -> {
                val builder = StringBuilder()
                while (true) {
                    builder.append(nextSibling.text)
                    nextSibling = nextSibling.nextSibling ?: break
                }
                KtPsiFactory(ifExpression).createBlock(builder.toString())
            }
        }
    }

    private fun KtIfExpression.siblingsUpTo(other: KtExpression): List<PsiElement> {
        val result = ArrayList<PsiElement>()
        var nextSibling = nextSibling
        // We delete elements up to the next if (or up to the end of the surrounding block)
        while (nextSibling != null && nextSibling != other) {
            // RBRACE closes the surrounding block, so it should not be copied / deleted
            if (nextSibling !is PsiWhiteSpace && nextSibling.node.elementType != KtTokens.RBRACE) {
                result.add(nextSibling)
            }
            nextSibling = nextSibling.nextSibling
        }
        return result
    }

    private fun BuilderByPattern<*>.appendElseBlock(block: KtExpression?) {
        appendFixedText("else->")
        appendExpression(block?.unwrapBlockOrParenthesis())
        appendFixedText("\n")
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val siblings = element.siblings()
        val elementCommentSaver = CommentSaver(element)
        val fullCommentSaver = CommentSaver(PsiChildRange(element, siblings.last()), saveLineBreaks = true)

        val toDelete = ArrayList<PsiElement>()
        var applyFullCommentSaver = true
        var whenExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("when {\n")

            var currentIfExpression = element
            var baseIfExpressionForSyntheticBranch = currentIfExpression
            var canPassThrough = false
            while (true) {
                val condition = currentIfExpression.condition
                val orBranches = ArrayList<KtExpression>()
                if (condition != null) {
                    orBranches.addOrBranches(condition)
                }

                appendExpressions(orBranches, separator = "||")

                appendFixedText("->")

                val currentThenBranch = currentIfExpression.then
                appendExpression(currentThenBranch?.unwrapBlockOrParenthesis())
                appendFixedText("\n")

                canPassThrough = canPassThrough || canPassThrough(currentThenBranch)

                val currentElseBranch = currentIfExpression.`else`
                if (currentElseBranch == null) {
                    // Try to build synthetic if / else according to KT-10750
                    val syntheticElseBranch = if (canPassThrough) break else buildNextBranch(baseIfExpressionForSyntheticBranch) ?: break
                    toDelete.addAll(baseIfExpressionForSyntheticBranch.siblingsUpTo(syntheticElseBranch))
                    if (syntheticElseBranch is KtIfExpression) {
                        baseIfExpressionForSyntheticBranch = syntheticElseBranch
                        currentIfExpression = syntheticElseBranch
                        toDelete.add(syntheticElseBranch)
                    }
                    else {
                        appendElseBlock(syntheticElseBranch)
                        break
                    }
                }
                else if (currentElseBranch is KtIfExpression) {
                    currentIfExpression = currentElseBranch
                }
                else {
                    appendElseBlock(currentElseBranch)
                    applyFullCommentSaver = false
                    break
                }
            }

            appendFixedText("}")
        } as KtWhenExpression


        if (whenExpression.getSubjectToIntroduce() != null) {
            whenExpression = whenExpression.introduceSubject()
        }

        val result = element.replace(whenExpression)
        (if (applyFullCommentSaver) fullCommentSaver else elementCommentSaver).restore(result)
        toDelete.forEach(PsiElement::delete)
    }

    private fun MutableList<KtExpression>.addOrBranches(expression: KtExpression): List<KtExpression> {
        if (expression is KtBinaryExpression && expression.operationToken == KtTokens.OROR) {
            val left = expression.left
            val right = expression.right
            if (left != null && right != null) {
                addOrBranches(left)
                addOrBranches(right)
                return this
            }
        }

        add(KtPsiUtil.safeDeparenthesize(expression, true))
        return this
    }
}
