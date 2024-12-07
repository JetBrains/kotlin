/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.StubTypeForBuilderInference
import org.jetbrains.kotlin.types.expressions.BasicExpressionTypingVisitor
import org.jetbrains.kotlin.types.isError

object BuilderInferenceAssignmentChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is PropertyDescriptor) return
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.NoBuilderInferenceWithoutAnnotationRestriction)) return
        val callElement = resolvedCall.call.callElement
        if (callElement !is KtNameReferenceExpression) return
        val binaryExpression = callElement.getParentOfType<KtBinaryExpression>(strict = true) ?: return
        if (binaryExpression.operationToken != KtTokens.EQ) return
        if (!BasicExpressionTypingVisitor.isLValue(callElement, binaryExpression)) return
        val right = binaryExpression.right ?: return

        if (resolvedCall.candidateDescriptor.returnType is StubTypeForBuilderInference) {
            val leftType = resultingDescriptor.returnType?.takeIf { !it.isError } ?: return
            val rightType = right.getType(context.trace.bindingContext) ?: return

            if (isAssignmentCorrectWithDataFlowInfo(leftType, right, rightType, context)) return
            context.trace.report(Errors.TYPE_MISMATCH.on(right, leftType, rightType))
        } else if (right is KtLambdaExpression) {
            val functionLiteral = right.functionLiteral
            val functionDescriptor = context.trace.get(BindingContext.FUNCTION, right.functionLiteral) ?: return
            for ((index, valueParameterDescriptor) in functionDescriptor.valueParameters.withIndex()) {
                if (valueParameterDescriptor.type !is StubTypeForBuilderInference) continue
                val target = functionLiteral.valueParameters.getOrNull(index) ?: functionLiteral
                context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(target, valueParameterDescriptor.name))
            }
        }
    }
}
