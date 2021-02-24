/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReplaceInvokeIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java,
    KotlinBundle.lazyMessage("replace.invoke.with.direct.call")
), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorNameConventions.INVOKE.asString() ||
            element.callExpression?.typeArgumentList != null ||
            (element.toResolvedCall(BodyResolveMode.PARTIAL)?.resultingDescriptor as? FunctionDescriptor)?.isOperator != true
        ) return null
        return element.callExpression?.calleeExpression?.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val receiver = element.receiverExpression
        val callExpression = element.callExpression?.copy() as? KtCallExpression ?: return
        callExpression.calleeExpression?.replace(receiver)
        element.replace(callExpression)
    }
}
