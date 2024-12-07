/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.util.isConventionCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class OperatorCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val functionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return
        if (!checkNotErrorOrDynamic(functionDescriptor)) return

        val element = resolvedCall.call.calleeExpression ?: resolvedCall.call.callElement
        val call = resolvedCall.call

        if (resolvedCall is VariableAsFunctionResolvedCall &&
            call is CallTransformer.CallForImplicitInvoke && call.itIsVariableAsFunctionCall) {
            val outerCall = call.outerCall
            if (isConventionCall(outerCall)) {
                val containingDeclarationName = functionDescriptor.containingDeclaration.fqNameUnsafe.asString()
                context.trace.report(Errors.PROPERTY_AS_OPERATOR.on(reportOn, functionDescriptor, containingDeclarationName))
            } else if (isWrongCallWithExplicitTypeArguments(resolvedCall, outerCall)) {
                context.trace.report(Errors.TYPE_ARGUMENTS_NOT_ALLOWED.on(reportOn as KtElement, "on implicit invoke call"))
            }
        }

        if (call.callElement is KtDestructuringDeclarationEntry || call is CallTransformer.CallForImplicitInvoke) {
            if (!functionDescriptor.isOperator) {
                report(reportOn, functionDescriptor, context.trace)
            }
            return
        }

        val isConventionOperator = element is KtOperationReferenceExpression && element.isConventionOperator()

        if (isConventionOperator || element is KtArrayAccessExpression) {
            if (!functionDescriptor.isOperator) {
                report(reportOn, functionDescriptor, context.trace)
            }
        }
    }

    companion object {
        fun report(reportOn: PsiElement, descriptor: FunctionDescriptor, sink: DiagnosticSink) {
            if (!checkNotErrorOrDynamic(descriptor)) return

            val containingDeclaration = descriptor.containingDeclaration
            val containingDeclarationName = containingDeclaration.fqNameUnsafe.asString()
            sink.report(Errors.OPERATOR_MODIFIER_REQUIRED.on(reportOn, descriptor, containingDeclarationName))
        }

        private fun checkNotErrorOrDynamic(functionDescriptor: FunctionDescriptor): Boolean {
            return !functionDescriptor.isDynamic() && !ErrorUtils.isError(functionDescriptor)
        }

        private fun isWrongCallWithExplicitTypeArguments(
            resolvedCall: VariableAsFunctionResolvedCall,
            outerCall: Call
        ): Boolean {
            val passedTypeArgumentsToInvoke = outerCall.typeArguments.isNotEmpty() &&
                    resolvedCall.functionCall.candidateDescriptor.typeParameters.isNotEmpty()
            return passedTypeArgumentsToInvoke && resolvedCall.variableCall.candidateDescriptor.typeParameters.isNotEmpty()
        }
    }
}
