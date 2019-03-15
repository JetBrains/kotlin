/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CAPTURED_IN_CLOSURE
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.expressions.CaptureKind

class CapturingInClosureChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val variableResolvedCall = (resolvedCall as? VariableAsFunctionResolvedCall)?.variableCall ?: resolvedCall
        val variableDescriptor = variableResolvedCall.resultingDescriptor as? VariableDescriptor
        if (variableDescriptor != null) {
            checkCapturingInClosure(variableDescriptor, context.trace, context.scope)
        }
    }

    private fun checkCapturingInClosure(variable: VariableDescriptor, trace: BindingTrace, scope: LexicalScope) {
        val variableParent = variable.containingDeclaration
        val scopeContainer = scope.ownerDescriptor
        if (isCapturedVariable(variableParent, scopeContainer)) {
            if (trace.get(CAPTURED_IN_CLOSURE, variable) != CaptureKind.NOT_INLINE) {
                trace.record(CAPTURED_IN_CLOSURE, variable, getCaptureKind(trace.bindingContext, scopeContainer, variableParent))
            }
        }
    }

    private fun isCapturedVariable(variableParent: DeclarationDescriptor, scopeContainer: DeclarationDescriptor): Boolean {
        if (variableParent !is FunctionDescriptor || scopeContainer == variableParent) return false

        if (variableParent is ConstructorDescriptor) {
            val classDescriptor = variableParent.containingDeclaration

            if (scopeContainer == classDescriptor) return false
            if (scopeContainer is PropertyDescriptor && scopeContainer.containingDeclaration == classDescriptor) return false
        }
        return true
    }

    private fun getCaptureKind(
        context: BindingContext, scopeContainer: DeclarationDescriptor, variableParent: DeclarationDescriptor
    ): CaptureKind {
        val scopeDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(scopeContainer)
        if (!InlineUtil.canBeInlineArgument(scopeDeclaration)) return CaptureKind.NOT_INLINE

        val exactlyOnceContract = isExactlyOnceContract(context, scopeDeclaration as KtFunction)
        if (InlineUtil.isInlinedArgument(scopeDeclaration, context, exactlyOnceContract)) {
            val scopeContainerParent = scopeContainer.containingDeclaration ?: error("parent is null for $scopeContainer")
            return if (
                !isCapturedVariable(variableParent, scopeContainerParent) ||
                getCaptureKind(context, scopeContainerParent, variableParent) == CaptureKind.INLINE_ONLY
            ) CaptureKind.INLINE_ONLY else CaptureKind.NOT_INLINE
        }
        if (exactlyOnceContract) return CaptureKind.EXACTLY_ONCE_EFFECT
        return CaptureKind.NOT_INLINE
    }

    private fun isExactlyOnceContract(bindingContext: BindingContext, argument: KtFunction): Boolean {
        val call = KtPsiUtil.getParentCallIfPresent(argument) ?: return false
        val resolvedCall = call.getResolvedCall(bindingContext) ?: return false
        val descriptor = resolvedCall.getResultingDescriptor()
        val valueArgument = resolvedCall.call.getValueArgumentForExpression(argument) ?: return false
        val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return false
        val parameter = mapping.valueParameter
        val contractDescription = descriptor.getUserData(ContractProviderKey)?.getContractDescription() ?: return false
        val effect = contractDescription.effects.filterIsInstance<CallsEffectDeclaration>()
            .find { it.variableReference.descriptor == parameter } ?: return false
        return effect.kind == InvocationKind.EXACTLY_ONCE
    }
}
