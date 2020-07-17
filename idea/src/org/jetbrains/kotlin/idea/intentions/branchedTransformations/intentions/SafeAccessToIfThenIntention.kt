/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.ComplexRedundantLetInspection
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
    KotlinBundle.lazyMessage("replace.safe.access.expression.with.if.expression")
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
        val redundantLetInspection = ComplexRedundantLetInspection()
        if (!redundantLetInspection.isApplicable(callExpression)) return false
        redundantLetInspection.applyTo(callExpression, project, editor)
        return true
    }

}
