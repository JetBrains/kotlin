/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ReplaceCallWithUnaryOperatorIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java,
    KotlinBundle.lazyMessage("replace.call.with.unary.operator")
), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val operation = operation(element.calleeName) ?: return null
        if (!isApplicableOperation(operation)) return null

        val call = element.callExpression ?: return null
        if (call.typeArgumentList != null) return null
        if (call.valueArguments.isNotEmpty()) return null

        if (!element.isReceiverExpressionWithValue()) return null

        setTextGetter(KotlinBundle.lazyMessage("replace.with.0.operator", operation.value))
        return call.calleeExpression?.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val operation = operation(element.calleeName)?.value ?: return
        val receiver = element.receiverExpression
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0$1", operation, receiver))
    }

    private fun isApplicableOperation(operation: KtSingleValueToken): Boolean = operation !in OperatorConventions.INCREMENT_OPERATIONS

    private fun operation(functionName: String?): KtSingleValueToken? = functionName?.let {
        OperatorConventions.UNARY_OPERATION_NAMES.inverse()[Name.identifier(it)]
    }
}
