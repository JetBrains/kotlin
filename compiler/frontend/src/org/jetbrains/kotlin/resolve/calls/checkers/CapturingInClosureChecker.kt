/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
                trace.record(CAPTURED_IN_CLOSURE, variable, getCaptureKind(trace.bindingContext, scopeContainer, variableParent, variable))
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
        context: BindingContext,
        scopeContainer: DeclarationDescriptor,
        variableParent: DeclarationDescriptor,
        variable: VariableDescriptor
    ): CaptureKind {
        val scopeDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(scopeContainer)
        if (!InlineUtil.canBeInlineArgument(scopeDeclaration)) return CaptureKind.NOT_INLINE

        if (InlineUtil.isInlinedArgument(scopeDeclaration as KtFunction, context, false) &&
            !isCrossinlineParameter(context, scopeDeclaration)
        ) {
            val scopeContainerParent = scopeContainer.containingDeclaration ?: error("parent is null for $scopeContainer")
            return if (
                !isCapturedVariable(variableParent, scopeContainerParent) ||
                getCaptureKind(context, scopeContainerParent, variableParent, variable) == CaptureKind.INLINE_ONLY
            ) CaptureKind.INLINE_ONLY else CaptureKind.NOT_INLINE
        }
        val exactlyOnceContract = isExactlyOnceContract(context, scopeDeclaration)
        // We cannot box arguments.
        val isArgument = variable is ValueParameterDescriptor && variableParent is CallableDescriptor
                && variableParent.valueParameters.contains(variable)
        return if (exactlyOnceContract && !isArgument) CaptureKind.EXACTLY_ONCE_EFFECT else CaptureKind.NOT_INLINE
    }

    private fun isExactlyOnceParameter(function: DeclarationDescriptor, parameter: VariableDescriptor): Boolean {
        if (function !is CallableDescriptor) return false
        if (parameter !is ValueParameterDescriptor) return false
        val contractDescription = function.getUserData(ContractProviderKey)?.getContractDescription() ?: return false
        val effect = contractDescription.effects.filterIsInstance<CallsEffectDeclaration>()
            .find { it.variableReference.descriptor == parameter.original } ?: return false
        return effect.kind == InvocationKind.EXACTLY_ONCE
    }

    private fun isExactlyOnceContract(bindingContext: BindingContext, argument: KtFunction): Boolean {
        val (descriptor, parameter) = getCalleeDescriptorAndParameter(bindingContext, argument) ?: return false
        return isExactlyOnceParameter(descriptor, parameter)
    }

    private fun getCalleeDescriptorAndParameter(
        bindingContext: BindingContext,
        argument: KtFunction
    ): Pair<CallableDescriptor, ValueParameterDescriptor>? {
        val call = KtPsiUtil.getParentCallIfPresent(argument) ?: return null
        val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
        val descriptor = resolvedCall.resultingDescriptor
        val valueArgument = resolvedCall.call.getValueArgumentForExpression(argument) ?: return null
        val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
        val parameter = mapping.valueParameter
        return descriptor to parameter
    }

    private fun isCrossinlineParameter(bindingContext: BindingContext, argument: KtFunction): Boolean {
        return getCalleeDescriptorAndParameter(bindingContext, argument)?.second?.isCrossinline == true
    }
}
