/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.completion.canBeUsedWithoutNameInCall
import org.jetbrains.kotlin.idea.completion.placedOnItsOwnPositionInCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class RemoveArgumentNameIntention : SelfTargetingRangeIntention<KtValueArgument>(KtValueArgument::class.java, "Remove argument name") {
    override fun applicabilityRange(element: KtValueArgument): TextRange? {
        if (!element.isNamed()) return null
        val resolvedCall = element.getStrictParentOfType<KtCallElement>()?.resolveToCall() ?: return null

        if (!element.placedOnItsOwnPositionInCall(resolvedCall)) return null
        if (!element.canBeUsedWithoutNameInCall(resolvedCall)) return null

        val argumentExpression = element.getArgumentExpression() ?: return null
        return TextRange(element.startOffset, argumentExpression.startOffset)
    }

    override fun applyTo(element: KtValueArgument, editor: Editor?) {
        val argumentExpr = element.getArgumentExpression() ?: return
        val argumentList = element.parent as? KtValueArgumentList ?: return
        val resolvedCall = (argumentList.parent as? KtCallElement)?.resolveToCall() ?: return
        val psiFactory = KtPsiFactory(element)
        if (argumentExpr is KtCollectionLiteralExpression && resolvedCall.getParameterForArgument(element)?.isVararg == true) {
            argumentExpr.getInnerExpressions()
                .map { psiFactory.createArgument(it) }
                .reversed()
                .forEach { argumentList.addArgumentAfter(it, element) }
            argumentList.removeArgument(element)
        } else {
            val newArgument = psiFactory.createArgument(argumentExpr, null, element.getSpreadElement() != null)
            element.replace(newArgument)
        }
    }
}