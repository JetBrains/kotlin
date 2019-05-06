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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.ReplaceSingleLineLetIntention
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableSimpleExpression
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SafeAccessToIfThenIntention : SelfTargetingRangeIntention<KtSafeQualifiedExpression>(
    KtSafeQualifiedExpression::class.java,
    "Replace safe access expression with 'if' expression"
), LowPriorityAction {
    override fun applicabilityRange(element: KtSafeQualifiedExpression): TextRange? {
        if (element.selectorExpression == null) return null
        return element.operationTokenNode.textRange
    }

    override fun applyTo(element: KtSafeQualifiedExpression, editor: Editor?) {
        val receiver = KtPsiUtil.safeDeparenthesize(element.receiverExpression)
        val selector = element.selectorExpression!!

        val receiverIsStable = receiver.isStableSimpleExpression()

        val psiFactory = KtPsiFactory(element)
        val dotQualified = psiFactory.createExpressionByPattern("$0.$1", receiver, selector)

        val elseClause = if (element.isUsedAsStatement(element.analyze())) null else psiFactory.createExpression("null")
        var ifExpression = element.convertToIfNotNullExpression(receiver, dotQualified, elseClause)

        var isAssignment = false
        val binaryExpression = (ifExpression.parent as? KtParenthesizedExpression)?.parent as? KtBinaryExpression
        val right = binaryExpression?.right
        if (right != null && binaryExpression.operationToken == KtTokens.EQ) {
            val replaced = binaryExpression.replaced(psiFactory.createExpressionByPattern("$0 = $1", ifExpression.text, right))
            ifExpression = replaced.findDescendantOfType()!!
            isAssignment = true
        }

        val isRedundantLetCallRemoved = ifExpression.removeRedundantLetCallIfPossible(editor)

        if (!receiverIsStable) {
            val valueToExtract = when {
                isAssignment ->
                    ((ifExpression.then as? KtBinaryExpression)?.left as? KtDotQualifiedExpression)?.receiverExpression
                isRedundantLetCallRemoved -> {
                    val context = ifExpression.analyze(BodyResolveMode.PARTIAL)
                    val descriptor = (ifExpression.condition as? KtBinaryExpression)?.left?.getResolvedCall(context)?.resultingDescriptor
                    ifExpression.then?.findDescendantOfType<KtNameReferenceExpression> {
                        it.getReferencedNameAsName() == descriptor?.name && it.getResolvedCall(context)?.resultingDescriptor == descriptor
                    }
                }
                else ->
                    (ifExpression.then as? KtDotQualifiedExpression)?.receiverExpression
            }
            if (valueToExtract != null) ifExpression.introduceValueForCondition(valueToExtract, editor)
        }
    }

    private fun KtIfExpression.removeRedundantLetCallIfPossible(editor: Editor?): Boolean {
        val callExpression = (then as? KtQualifiedExpression)?.callExpression ?: return false
        if (callExpression.calleeExpression?.text != "let") return false
        val replaceSingleLineLetIntention = ReplaceSingleLineLetIntention()
        if (!replaceSingleLineLetIntention.isApplicableTo(callExpression)) return false
        replaceSingleLineLetIntention.applyTo(callExpression, editor)
        return true
    }

}
