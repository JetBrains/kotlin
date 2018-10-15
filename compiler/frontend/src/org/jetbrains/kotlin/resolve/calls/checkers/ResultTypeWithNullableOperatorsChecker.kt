/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.isResultType
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils

class ResultTypeWithNullableOperatorsChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val name = resolvedCall.resultingDescriptor.name
        val operationNode = resolvedCall.call.callOperationNode

        when {
            operationNode?.elementType == KtTokens.SAFE_ACCESS -> {
                val receiver = resolvedCall.resultingDescriptor.dispatchReceiverParameter ?: return
                if (receiver.type.isResultType()) {
                    context.trace.report(Errors.RESULT_CLASS_WITH_NULLABLE_OPERATOR.on(operationNode!!.psi, "?."))
                }
            }

            name == ControlStructureTypingUtils.ResolveConstruct.EXCL_EXCL.specialFunctionName -> {
                if (resolvedCall.resultingDescriptor.returnType?.isResultType() == true) {
                    context.trace.report(Errors.RESULT_CLASS_WITH_NULLABLE_OPERATOR.on(reportOn, "!!"))
                }
            }

            name == ControlStructureTypingUtils.ResolveConstruct.ELVIS.specialFunctionName -> {
                val elvisBinaryExpression = resolvedCall.call.callElement as? KtBinaryExpression ?: return
                val left = elvisBinaryExpression.left ?: return
                val leftType = context.trace.getType(left) ?: return

                if (leftType.isResultType()) {
                    context.trace.reportDiagnosticOnce(Errors.RESULT_CLASS_WITH_NULLABLE_OPERATOR.on(reportOn, "?:"))
                }

                // Additional check for case `a ?: b ?: c`, where `b` is Result
                // This is needed because inference will give common supertype for `a ?: b` which might not be Result
                if (left is KtBinaryExpression) {
                    val lastExpression = left.right ?: return
                    val lastExpressionType = context.trace.getType(lastExpression) ?: return

                    if (lastExpressionType.isResultType()) {
                        context.trace.reportDiagnosticOnce(Errors.RESULT_CLASS_WITH_NULLABLE_OPERATOR.on(reportOn, "?:"))
                    }
                }
            }
        }
    }
}