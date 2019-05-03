/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.typeUtil.contains

class UselessElvisCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor.name != ControlStructureTypingUtils.ResolveConstruct.ELVIS.specialFunctionName) return

        val elvisBinaryExpression = resolvedCall.call.callElement as? KtBinaryExpression ?: return
        val left = elvisBinaryExpression.left ?: return
        val right = elvisBinaryExpression.right ?: return

        val leftType = context.trace.getType(left) ?: return

        // if type contains not fixed `TypeVariable` it means that call wasn't completed, we should wait for its completion first
        if (leftType.isError || leftType.contains { it.constructor is TypeVariableTypeConstructor }) return

        if (!TypeUtils.isNullableType(leftType)) {
            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

        val dataFlowValue = context.dataFlowValueFactory.createDataFlowValue(left, leftType, context.resolutionContext)
        if (context.dataFlowInfo.getStableNullability(dataFlowValue) == Nullability.NOT_NULL) {
            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

        if (KtPsiUtil.isNullConstant(right) && !leftType.isNullabilityFlexible()) {
            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS_RIGHT_IS_NULL.on(elvisBinaryExpression))
        }
    }
}