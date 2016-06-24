/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isConventionCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions.MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_PLUS

class OperatorCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext, languageFeatureSettings: LanguageFeatureSettings) {
        val functionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return
        if (!checkNotErrorOrDynamic(functionDescriptor)) return

        val element = resolvedCall.call.calleeExpression ?: resolvedCall.call.callElement
        val call = resolvedCall.call

        fun isInvokeCall(): Boolean = call is CallTransformer.CallForImplicitInvoke

        fun isMultiDeclaration(): Boolean = call.callElement is KtDestructuringDeclarationEntry

        if (resolvedCall is VariableAsFunctionResolvedCall &&
            call is CallTransformer.CallForImplicitInvoke && call.itIsVariableAsFunctionCall) {
            val outerCall = call.outerCall
            if (isConventionCall(outerCall)) {
                throw AssertionError("Illegal resolved call to variable with invoke for $outerCall. " +
                                     "Variable: ${resolvedCall.variableCall.resultingDescriptor}")
            }
        }

        if (isMultiDeclaration() || isInvokeCall()) {
            if (!functionDescriptor.isOperator) {
                report(call.callElement, functionDescriptor, context.trace)
            }
            return
        }

        val isConventionOperator = element is KtOperationReferenceExpression && element.getNameForConventionalOperation() != null
        if (isConventionOperator || element is KtArrayAccessExpression) {
            if (!functionDescriptor.isOperator) {
                report(element, functionDescriptor, context.trace)
            }
            if (isConventionOperator) {
                checkDeprecatedUnaryConventions(call, functionDescriptor, context.trace)
            }
        }
    }

    private fun checkDeprecatedUnaryConventions(call: Call, functionDescriptor: FunctionDescriptor, sink: DiagnosticSink) {
        (call.callElement as? KtPrefixExpression)?.let { expr ->
            val functionName = functionDescriptor.name
            if (functionName == PLUS || functionName == MINUS) {
                val newName = if (functionName == PLUS) UNARY_PLUS else UNARY_MINUS
                sink.report(Errors.DEPRECATED_UNARY_PLUS_MINUS.on(expr, functionDescriptor, newName.asString()))
            }
        }
    }

    companion object {
        fun report(element: PsiElement, descriptor: FunctionDescriptor, sink: DiagnosticSink) {
            if (!checkNotErrorOrDynamic(descriptor)) return

            val containingDeclaration = descriptor.containingDeclaration
            val containingDeclarationName = containingDeclaration.fqNameUnsafe.asString()
            sink.report(Errors.OPERATOR_MODIFIER_REQUIRED.on(element, descriptor, containingDeclarationName))
        }

        private fun checkNotErrorOrDynamic(functionDescriptor: FunctionDescriptor): Boolean {
            return (!functionDescriptor.isDynamic() && !ErrorUtils.isError(functionDescriptor))
        }
    }
}
