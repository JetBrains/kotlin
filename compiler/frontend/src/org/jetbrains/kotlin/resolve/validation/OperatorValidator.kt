/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_MINUS

public class OperatorValidator : SymbolUsageValidator {

    override fun validateCall(resolvedCall: ResolvedCall<*>?, targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        val functionDescriptor = targetDescriptor as? FunctionDescriptor ?: return
        if (!checkNotErrorOrDynamic(functionDescriptor)) return

        val jetElement = element as? KtElement ?: return
        val call = resolvedCall?.call ?: trace.bindingContext[BindingContext.CALL, jetElement]

        fun isInvokeCall(): Boolean {
            return call is CallTransformer.CallForImplicitInvoke
        }

        fun isMultiDeclaration(): Boolean {
            return (resolvedCall != null) && (call?.callElement is KtMultiDeclarationEntry)
        }

        fun isConventionOperator(): Boolean {
            if (jetElement !is KtOperationReferenceExpression) return false
            return jetElement.getNameForConventionalOperation() != null
        }

        fun isArrayAccessExpression() = jetElement is KtArrayAccessExpression

        if (isMultiDeclaration() || isInvokeCall()) {
            if (!functionDescriptor.isOperator && call != null) {
                report(call.callElement, functionDescriptor, trace)
            }
            return
        }

        val isConventionOperator = isConventionOperator()
        if (isConventionOperator || isArrayAccessExpression()) {
            if (!functionDescriptor.isOperator) {
                report(jetElement, functionDescriptor, trace)
            }
            if (isConventionOperator && call != null) {
                checkDeprecatedUnaryConventions(call, functionDescriptor, trace)
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