/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors.CAPTURED_VAL_INITIALIZATION
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
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
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.expressions.CaptureKind

class CapturingInClosureChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val variableResolvedCall = (resolvedCall as? VariableAsFunctionResolvedCall)?.variableCall ?: resolvedCall
        val variableDescriptor = variableResolvedCall.resultingDescriptor as? VariableDescriptor
        if (variableDescriptor != null) {
            checkCapturingInClosure(variableDescriptor, context.trace, context.scope)
            checkFieldInExactlyOnceLambdaInitialization(variableDescriptor, context.trace, context.scope.ownerDescriptor, reportOn)
        }
    }

    private fun checkCapturingInClosure(variable: VariableDescriptor, trace: BindingTrace, scope: LexicalScope) {
        val variableParent = variable.containingDeclaration
        val scopeContainer = scope.ownerDescriptor
        if (isCapturedVariable(variableParent, scopeContainer)) {
            if (trace.get(CAPTURED_IN_CLOSURE, variable) != CaptureKind.NOT_INLINE) {
                trace.record(CAPTURED_IN_CLOSURE, variable, getCaptureKind(trace.bindingContext, scopeContainer, variableParent, variable))
                return
            }
        }
    }

    private fun checkFieldInExactlyOnceLambdaInitialization(
        variable: VariableDescriptor,
        trace: BindingTrace,
        scopeContainer: DeclarationDescriptor,
        nameElement: PsiElement
    ) {
        if (variable !is PropertyDescriptor || scopeContainer !is AnonymousFunctionDescriptor || variable.isVar) return
        if (!isLhsOfAssignment(nameElement as KtExpression)) return
        val scopeDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(scopeContainer) as? KtFunction ?: return
        if (scopeContainer.containingDeclaration !is ConstructorDescriptor &&
            scopeContainer.containingDeclaration !is PropertyDescriptor
        ) return
        if (!isExactlyOnceContract(trace.bindingContext, scopeDeclaration)) return
        if (trace.bindingContext[CAPTURED_IN_CLOSURE, variable] == CaptureKind.NOT_INLINE) return
        val (callee, param) = getCalleeDescriptorAndParameter(trace.bindingContext, scopeDeclaration) ?: return
        if (callee !is FunctionDescriptor) return
        if (!callee.isInline || (param.isCrossinline || !InlineUtil.isInlineParameter(param))) {
            trace.report(CAPTURED_VAL_INITIALIZATION.on(nameElement, variable))
        }
    }

    private fun isLhsOfAssignment(nameElement: KtExpression): Boolean {
        val parent = nameElement.parent as? KtBinaryExpression ?: return false
        return parent.operationToken == KtTokens.EQ && parent.left == nameElement
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
        if (!exactlyOnceContract) return CaptureKind.NOT_INLINE
        // We cannot box function/lambda arguments, destructured lambda arguments, for-loop index variables,
        // exceptions inside catch blocks ans vals in when
        return if (isArgument(variable, variableParent) ||
            findDestructuredVariable(variable, variableParent) != null ||
            isForLoopParameter(variable) ||
            isCatchBlockParameter(variable) ||
            isValInWhen(variable)
        ) {
            CaptureKind.NOT_INLINE
        } else {
            CaptureKind.EXACTLY_ONCE_EFFECT
        }
    }

    private fun isArgument(variable: VariableDescriptor, variableParent: DeclarationDescriptor): Boolean =
        variable is ValueParameterDescriptor && variableParent is CallableDescriptor
                && variableParent.valueParameters.contains(variable)

    private fun isValInWhen(variable: VariableDescriptor): Boolean {
        val psi = ((variable as? LocalVariableDescriptor)?.source as? KotlinSourceElement)?.psi ?: return false
        return (psi.parent as? KtWhenExpression)?.let { it.subjectVariable == psi } == true
    }

    private fun isCatchBlockParameter(variable: VariableDescriptor): Boolean {
        val psi = ((variable as? LocalVariableDescriptor)?.source as? KotlinSourceElement)?.psi ?: return false
        return psi.parent.parent.let { it is KtCatchClause && it.parameterList?.parameters?.contains(psi) == true }
    }

    private fun isForLoopParameter(variable: VariableDescriptor): Boolean {
        val psi = ((variable as? LocalVariableDescriptor)?.source as? KotlinSourceElement)?.psi ?: return false
        if (psi.parent is KtForExpression) {
            val forExpression = psi.parent as KtForExpression
            return forExpression.loopParameter == psi
        } else if (psi.parent is KtDestructuringDeclaration) {
            val parameter = psi.parent.parent as? KtParameter ?: return false
            val forExpression = parameter.parent as? KtForExpression ?: return false
            return forExpression.loopParameter == parameter
        }
        return false
    }

    private fun isExactlyOnceParameter(function: DeclarationDescriptor, parameter: VariableDescriptor): Boolean {
        if (function !is CallableDescriptor) return false
        if (parameter !is ValueParameterDescriptor) return false
        val contractDescription = function.getUserData(ContractProviderKey)?.getContractDescription() ?: return false
        val effect = contractDescription.effects.filterIsInstance<CallsEffectDeclaration>()
            .find { it.variableReference.descriptor == parameter.original } ?: return false
        return effect.kind == EventOccurrencesRange.EXACTLY_ONCE
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

fun findDestructuredVariable(variable: VariableDescriptor, variableParent: DeclarationDescriptor): ValueParameterDescriptor? =
    if (variable is LocalVariableDescriptor && variableParent is AnonymousFunctionDescriptor) {
        variableParent.valueParameters.find {
            it is ValueParameterDescriptorImpl.WithDestructuringDeclaration && it.destructuringVariables.contains(variable)
        }
    } else null
