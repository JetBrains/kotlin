/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.isSafeCall
import org.jetbrains.kotlin.resolve.isNullableUnderlyingType
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*

class RuntimeAssertionInfo(val needNotNullAssertion: Boolean, val message: String) {
    interface DataFlowExtras {
        val canBeNull: Boolean
        val presentableText: String
    }

    companion object {
        @JvmStatic
        fun create(
            expectedType: KotlinType,
            expressionType: KotlinType,
            dataFlowExtras: DataFlowExtras
        ): RuntimeAssertionInfo? {
            fun assertNotNull(): Boolean {
                if (expectedType.isError || expressionType.isError) return false

                // T : Any, T! = T..T?
                // Let T$ will be copy of T! with enhanced nullability.
                // Cases when nullability assertion needed: T! -> T, T$ -> T

                // Expected type either T?, T! or T$
                if (TypeUtils.isNullableType(expectedType) ||
                    expectedType.hasEnhancedNullability() ||
                    expectedType.isNullableUnderlyingType()
                ) {
                    return false
                }

                // Expression type is not nullable and not enhanced (neither T?, T! or T$)
                val isExpressionTypeNullable = TypeUtils.isNullableType(expressionType)
                if (!isExpressionTypeNullable && !expressionType.hasEnhancedNullability()) return false

                // Smart-cast T! or T?
                if (!dataFlowExtras.canBeNull && isExpressionTypeNullable) return false

                return true
            }

            return if (assertNotNull())
                RuntimeAssertionInfo(needNotNullAssertion = true, message = dataFlowExtras.presentableText)
            else
                null
        }
    }
}

private val KtExpression.textForRuntimeAssertionInfo
    get() = StringUtil.trimMiddle(text, 50)

class RuntimeAssertionsDataFlowExtras(
    private val c: ResolutionContext<*>,
    private val expressionType: KotlinType,
    private val expression: KtExpression
) : RuntimeAssertionInfo.DataFlowExtras {
    private val dataFlowValue by lazy(LazyThreadSafetyMode.PUBLICATION) {
        c.dataFlowValueFactory.createDataFlowValue(expression, expressionType, c)
    }

    override val canBeNull: Boolean
        get() = c.dataFlowInfo.getStableNullability(dataFlowValue).canBeNull()
    override val presentableText: String
        get() = expression.textForRuntimeAssertionInfo
}

object RuntimeAssertionsTypeChecker : AdditionalTypeChecker {
    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        if (TypeUtils.noExpectedType(c.expectedType) || c.expectedType is StubTypeForBuilderInference) return

        val assertionInfo = RuntimeAssertionInfo.create(
            c.expectedType,
            expressionType,
            RuntimeAssertionsDataFlowExtras(c, expressionType, expression)
        )

        if (assertionInfo != null) {
            c.trace.record(JvmBindingContextSlices.RUNTIME_ASSERTION_INFO, expression, assertionInfo)
        }
    }

}

object RuntimeAssertionsOnExtensionReceiverCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.call.isSafeCall()) return

        val callee = resolvedCall.resultingDescriptor
        checkReceiver(callee.extensionReceiverParameter, resolvedCall.extensionReceiver, context)
    }

    private fun checkReceiver(receiverParameter: ReceiverParameterDescriptor?, receiverValue: ReceiverValue?, context: CallCheckerContext) {
        if (receiverParameter == null || receiverValue == null) return
        val expressionReceiverValue = receiverValue as? ExpressionReceiver ?: return
        val receiverExpression = expressionReceiverValue.expression
        val c = context.resolutionContext

        val assertionInfo = RuntimeAssertionInfo.create(
            receiverParameter.type,
            receiverValue.type,
            RuntimeAssertionsDataFlowExtras(c, receiverValue.type, receiverExpression)
        )

        if (assertionInfo != null) {
            c.trace.record(JvmBindingContextSlices.RECEIVER_RUNTIME_ASSERTION_INFO, expressionReceiverValue, assertionInfo)
        }
    }
}
