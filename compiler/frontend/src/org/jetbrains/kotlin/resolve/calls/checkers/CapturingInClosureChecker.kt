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

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CAPTURED_IN_CLOSURE
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.expressions.CaptureKind

class CapturingInClosureChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val variableResolvedCall = if (resolvedCall is VariableAsFunctionResolvedCall) resolvedCall.variableCall else resolvedCall
        val variableDescriptor = variableResolvedCall.getResultingDescriptor() as? VariableDescriptor
        if (variableDescriptor != null) {
            checkCapturingInClosure(variableDescriptor, context.trace, context.scope)
        }
    }

    private fun checkCapturingInClosure(variable: VariableDescriptor, trace: BindingTrace, scope: JetScope) {
        val variableParent = variable.getContainingDeclaration()
        val scopeContainer = scope.getContainingDeclaration()
        if (isCapturedVariable(variableParent, scopeContainer)) {
            if (trace.get(CAPTURED_IN_CLOSURE, variable) != CaptureKind.NOT_INLINE) {
                val inline = isCapturedInInline(trace.getBindingContext(), scopeContainer, variableParent)
                trace.record(CAPTURED_IN_CLOSURE, variable, if (inline) CaptureKind.INLINE_ONLY else CaptureKind.NOT_INLINE)
            }
        }
    }

    private fun isCapturedVariable(variableParent: DeclarationDescriptor, scopeContainer: DeclarationDescriptor): Boolean {
        if (variableParent !is FunctionDescriptor || scopeContainer == variableParent) return false

        if (variableParent is ConstructorDescriptor) {
            val classDescriptor = variableParent.getContainingDeclaration()

            if (scopeContainer == classDescriptor) return false
            if (scopeContainer is PropertyDescriptor && scopeContainer.getContainingDeclaration() == classDescriptor) return false
        }
        return true
    }

    private fun isCapturedInInline(
            context: BindingContext, scopeContainer: DeclarationDescriptor, variableParent: DeclarationDescriptor
    ): Boolean {
        val scopeDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(scopeContainer)
        if (!InlineUtil.canBeInlineArgument(scopeDeclaration)) return false

        if (InlineUtil.isInlinedArgument(scopeDeclaration as JetFunction, context, false)) {
            val scopeContainerParent = scopeContainer.getContainingDeclaration()
            assert(scopeContainerParent != null) { "parent is null for " + scopeContainer }
            return !isCapturedVariable(variableParent, scopeContainerParent!!) || isCapturedInInline(context, scopeContainerParent, variableParent)
        }
        return false
    }
}


