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
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.util.OperatorNameConventions

import java.util.LinkedHashSet

import org.jetbrains.kotlin.diagnostics.Errors.NON_LOCAL_RETURN_NOT_ALLOWED
import org.jetbrains.kotlin.diagnostics.Errors.USAGE_IS_NOT_INLINABLE
import org.jetbrains.kotlin.resolve.inline.InlineUtil.allowsNonLocalReturns
import org.jetbrains.kotlin.resolve.inline.InlineUtil.checkNonLocalReturnUsage

internal class InlineChecker(private val descriptor: FunctionDescriptor) : CallChecker {
    private val inlinableParameters = LinkedHashSet<CallableDescriptor>()
    private val inlineFunEffectiveVisibility: EffectiveVisibility
    private val isEffectivelyPrivateApiFunction: Boolean

    init {
        assert(InlineUtil.isInline(descriptor)) { "This extension should be created only for inline functions: " + descriptor }
        this.inlineFunEffectiveVisibility = descriptor.effectiveVisibility(descriptor.visibility, true)
        this.isEffectivelyPrivateApiFunction = descriptor.isEffectivelyPrivateApi
        for (param in descriptor.valueParameters) {
            if (isInlinableParameter(param)) {
                inlinableParameters.add(param)
            }
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val expression = resolvedCall.call.calleeExpression ?: return

        //checking that only invoke or inlinable extension called on function parameter
        val targetDescriptor = resolvedCall.resultingDescriptor
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.dispatchReceiver, expression)
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.extensionReceiver, expression)

        if (inlinableParameters.contains(targetDescriptor)) {
            if (!isInsideCall(expression)) {
                context.trace.report(USAGE_IS_NOT_INLINABLE.on(expression, expression, descriptor))
            }
        }

        for ((valueDescriptor, value) in resolvedCall.valueArguments) {
            if (value !is DefaultValueArgument) {
                for (argument in value.arguments) {
                    checkValueParameter(context, targetDescriptor, argument, valueDescriptor)
                }
            }
        }

        checkVisibilityAndAccess(targetDescriptor, expression, context)
        checkRecursion(context, targetDescriptor, expression)
    }

    private fun isInsideCall(expression: KtExpression): Boolean {
        val parent = KtPsiUtil.getParentCallIfPresent(expression)
        if (parent is KtBinaryExpression) {
            val token = KtPsiUtil.getOperationToken((parent as KtOperationExpression?)!!)
            if (token === KtTokens.EQ || token === KtTokens.ANDAND || token === KtTokens.OROR) {
                //assignment
                return false
            }
        }

        if (parent != null) {
            //UGLY HACK
            //check there is no casts
            var current: PsiElement = expression
            while (current !== parent) {
                if (current is KtBinaryExpressionWithTypeRHS) {
                    return false
                }
                current = current.parent
            }
        }

        return parent != null
    }

    private fun checkValueParameter(
            context: CallCheckerContext,
            targetDescriptor: CallableDescriptor,
            targetArgument: ValueArgument,
            targetParameterDescriptor: ValueParameterDescriptor
    ) {
        val argumentExpression = targetArgument.getArgumentExpression() ?: return
        val argumentCallee = getCalleeDescriptor(context, argumentExpression, false)

        if (argumentCallee != null && inlinableParameters.contains(argumentCallee)) {
            if (InlineUtil.isInline(targetDescriptor) && isInlinableParameter(targetParameterDescriptor)) {
                if (allowsNonLocalReturns(argumentCallee) && !allowsNonLocalReturns(targetParameterDescriptor)) {
                    context.trace.report(NON_LOCAL_RETURN_NOT_ALLOWED.on(argumentExpression, argumentExpression))
                }
                else {
                    checkNonLocalReturn(context, argumentCallee, argumentExpression)
                }
            }
            else {
                context.trace.report(USAGE_IS_NOT_INLINABLE.on(argumentExpression, argumentExpression, descriptor))
            }
        }
    }

    private fun checkCallWithReceiver(
            context: CallCheckerContext,
            targetDescriptor: CallableDescriptor,
            receiver: ReceiverValue?,
            expression: KtExpression?
    ) {
        if (receiver == null) return

        val varDescriptor: CallableDescriptor?
        val receiverExpression: KtExpression?
        if (receiver is ExpressionReceiver) {
            receiverExpression = receiver.expression
            varDescriptor = getCalleeDescriptor(context, receiverExpression, true)
        }
        else if (receiver is ExtensionReceiver) {
            val extension = receiver.declarationDescriptor

            varDescriptor = extension.extensionReceiverParameter
            assert(varDescriptor != null) { "Extension should have receiverParameterDescriptor: " + extension }

            receiverExpression = expression
        }
        else {
            varDescriptor = null
            receiverExpression = null
        }

        if (inlinableParameters.contains(varDescriptor)) {
            //check that it's invoke or inlinable extension
            checkLambdaInvokeOrExtensionCall(context, varDescriptor!!, targetDescriptor, receiverExpression!!)
        }
    }

    private fun getCalleeDescriptor(
            context: CallCheckerContext,
            expression: KtExpression,
            unwrapVariableAsFunction: Boolean
    ): CallableDescriptor? {
        if (!(expression is KtSimpleNameExpression || expression is KtThisExpression)) return null

        val thisCall = expression.getResolvedCall(context.trace.bindingContext)
        if (unwrapVariableAsFunction && thisCall is VariableAsFunctionResolvedCall) {
            return (thisCall as VariableAsFunctionResolvedCall).variableCall.resultingDescriptor
        }
        return thisCall?.resultingDescriptor
    }

    private fun checkLambdaInvokeOrExtensionCall(
            context: CallCheckerContext,
            lambdaDescriptor: CallableDescriptor,
            callDescriptor: CallableDescriptor,
            receiverExpression: KtExpression
    ) {
        val inlinableCall = isInvokeOrInlineExtension(callDescriptor)
        if (!inlinableCall) {
            context.trace.report(USAGE_IS_NOT_INLINABLE.on(receiverExpression, receiverExpression, descriptor))
        }
        else {
            checkNonLocalReturn(context, lambdaDescriptor, receiverExpression)
        }
    }

    private fun checkRecursion(
            context: CallCheckerContext,
            targetDescriptor: CallableDescriptor,
            expression: KtElement
    ) {
        if (targetDescriptor.original === descriptor) {
            context.trace.report(Errors.RECURSION_IN_INLINE.on(expression, expression, descriptor))
        }
    }

    private fun isInlinableParameter(descriptor: ParameterDescriptor): Boolean {
        return InlineUtil.isInlineLambdaParameter(descriptor) && !descriptor.type.isMarkedNullable
    }

    private fun isInvokeOrInlineExtension(descriptor: CallableDescriptor): Boolean {
        if (descriptor !is SimpleFunctionDescriptor) {
            return false
        }

        val containingDeclaration = descriptor.getContainingDeclaration()
        val isInvoke = descriptor.getName() == OperatorNameConventions.INVOKE &&
                       containingDeclaration is ClassDescriptor &&
                       containingDeclaration.defaultType.isFunctionType

        return isInvoke || InlineUtil.isInline(descriptor)
    }

    private fun checkVisibilityAndAccess(
            calledDescriptor: CallableDescriptor,
            expression: KtElement,
            context: CallCheckerContext
    ) {
        val calledFunEffectiveVisibility = if (isDefinedInInlineFunction(calledDescriptor))
            EffectiveVisibility.Public
        else
            calledDescriptor.effectiveVisibility(calledDescriptor.visibility, true)

        val isCalledFunPublicOrPublishedApi = calledFunEffectiveVisibility.publicApi
        val isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.publicApi
        if (isInlineFunPublicOrPublishedApi &&
            !isCalledFunPublicOrPublishedApi &&
            calledDescriptor.visibility !== Visibilities.LOCAL) {
            context.trace.report(Errors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE.on(expression, calledDescriptor, descriptor))
        }
        else {
            checkPrivateClassMemberAccess(calledDescriptor, expression, context)
        }

        if (calledDescriptor !is ConstructorDescriptor &&
            isInlineFunPublicOrPublishedApi &&
            inlineFunEffectiveVisibility.toVisibility() !== Visibilities.PROTECTED &&
            calledFunEffectiveVisibility.toVisibility() === Visibilities.PROTECTED) {
            context.trace.report(Errors.PROTECTED_CALL_FROM_PUBLIC_INLINE.on(expression, calledDescriptor))
        }
    }

    private fun checkPrivateClassMemberAccess(
            declarationDescriptor: DeclarationDescriptor,
            expression: KtElement,
            context: CallCheckerContext
    ) {
        if (!isEffectivelyPrivateApiFunction) {
            if (declarationDescriptor.isInsidePrivateClass) {
                context.trace.report(Errors.PRIVATE_CLASS_MEMBER_FROM_INLINE.on(expression, declarationDescriptor, descriptor))
            }
        }
    }

    private fun isDefinedInInlineFunction(startDescriptor: DeclarationDescriptorWithVisibility): Boolean {
        var parent: DeclarationDescriptorWithVisibility? = startDescriptor

        while (parent != null) {
            if (parent.containingDeclaration === descriptor) return true

            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility::class.java)
        }

        return false
    }

    private fun checkNonLocalReturn(
            context: CallCheckerContext,
            inlinableParameterDescriptor: CallableDescriptor,
            parameterUsage: KtExpression
    ) {
        if (!allowsNonLocalReturns(inlinableParameterDescriptor)) return

        if (!checkNonLocalReturnUsage(descriptor, parameterUsage, context.resolutionContext)) {
            context.trace.report(NON_LOCAL_RETURN_NOT_ALLOWED.on(parameterUsage, parameterUsage))
        }
    }
}
