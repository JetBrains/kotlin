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
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.parsing.isFromContractDsl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getSuperCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.isInsidePrivateClass
import org.jetbrains.kotlin.resolve.descriptorUtil.isMemberOfCompanionOfPrivateClass
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.allowsNonLocalReturns
import org.jetbrains.kotlin.resolve.inline.InlineUtil.checkNonLocalReturnUsage
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.properties.Delegates

internal class InlineChecker(private val descriptor: FunctionDescriptor) : CallChecker {
    init {
        assert(InlineUtil.isInline(descriptor)) { "This extension should be created only for inline functions: $descriptor" }
    }

    private val inlineFunEffectiveVisibility = descriptor.effectiveVisibility(descriptor.visibility, true)

    private val isEffectivelyPrivateApiFunction = descriptor.isEffectivelyPrivateApi

    private val inlinableParameters = descriptor.valueParameters.filter { InlineUtil.isInlineParameter(it) }

    private val inlinableKtParameters = inlinableParameters.mapNotNull { (it.source as? KotlinSourceElement)?.psi }

    private var supportDefaultValueInline by Delegates.notNull<Boolean>()

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val call = resolvedCall.call
        val expression = call.calleeExpression ?: return

        supportDefaultValueInline = context.languageVersionSettings.supportsFeature(LanguageFeature.InlineDefaultFunctionalParameters)

        //checking that only invoke or inlinable extension called on function parameter
        val targetDescriptor = resolvedCall.resultingDescriptor

        // Omit inline checks for 'contract'-call because those calls will never be executed, so inline checking is pointless
        if (targetDescriptor.isFromContractDsl()) return

        checkCallWithReceiver(context, targetDescriptor, resolvedCall.dispatchReceiver, expression)
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.extensionReceiver, expression)

        if (inlinableParameters.contains(targetDescriptor)) {
            when {
                !checkNotInDefaultParameter(context, expression) -> { /*error*/
                }
                !isInsideCall(expression) -> context.trace.report(USAGE_IS_NOT_INLINABLE.on(expression, expression, descriptor))
            }
        }

        for ((valueDescriptor, value) in resolvedCall.valueArguments) {
            if (value !is DefaultValueArgument) {
                for (argument in value.arguments) {
                    checkValueParameter(context, targetDescriptor, argument, valueDescriptor)
                }
            }
        }

        val replacementForReport = (call.dispatchReceiver as? ExpressionReceiver)?.expression
        checkVisibilityAndAccess(targetDescriptor, expression, replacementForReport, context, call)
        checkRecursion(context, targetDescriptor, expression, replacementForReport)
    }

    private fun checkNotInDefaultParameter(context: CallCheckerContext, expression: KtExpression) =
        !supportDefaultValueInline || expression.getParentOfType<KtParameter>(true)?.let {
            val allow = it !in inlinableKtParameters
            if (!allow) {
                context.trace.report(
                    NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE.on(
                        expression,
                        expression,
                        descriptor
                    )
                )
            }
            allow
        } ?: true

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
            when {
                !checkNotInDefaultParameter(context, argumentExpression) -> { /*error*/
                }

                InlineUtil.isInline(targetDescriptor) && InlineUtil.isInlineParameter(targetParameterDescriptor) ->
                    if (allowsNonLocalReturns(argumentCallee) && !allowsNonLocalReturns(targetParameterDescriptor)) {
                        context.trace.report(NON_LOCAL_RETURN_NOT_ALLOWED.on(argumentExpression, argumentExpression))
                    } else {
                        checkNonLocalReturn(context, argumentCallee, argumentExpression)
                    }

                else -> context.trace.report(USAGE_IS_NOT_INLINABLE.on(argumentExpression, argumentExpression, descriptor))
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
        when (receiver) {
            is ExpressionReceiver -> {
                receiverExpression = receiver.expression
                varDescriptor = getCalleeDescriptor(context, receiverExpression, true)
            }
            is ExtensionReceiver -> {
                val extension = receiver.declarationDescriptor

                varDescriptor = extension.extensionReceiverParameter
                assert(varDescriptor != null) { "Extension should have receiverParameterDescriptor: " + extension }

                receiverExpression = expression
            }
            else -> {
                varDescriptor = null
                receiverExpression = null
            }
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
            if (InlineUtil.isInline(callDescriptor) &&
                !context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidExtensionCallsOnInlineFunctionalParameters)
            ) {
                context.trace.report(USAGE_IS_NOT_INLINABLE_WARNING.on(receiverExpression, receiverExpression, descriptor))
            } else {
                context.trace.report(USAGE_IS_NOT_INLINABLE.on(receiverExpression, receiverExpression, descriptor))
            }
        } else {
            checkNonLocalReturn(context, lambdaDescriptor, receiverExpression)
        }
    }

    private fun checkRecursion(
        context: CallCheckerContext,
        targetDescriptor: CallableDescriptor,
        expression: KtElement,
        replacementForReport: KtElement?
    ) {
        if (targetDescriptor.original === descriptor) {
            context.trace.report(RECURSION_IN_INLINE.on(expression, expression, descriptor))
            context.reportDeprecationOnReplacement(expression, replacementForReport)
        }
    }

    private fun isInvokeOrInlineExtension(descriptor: CallableDescriptor): Boolean {
        if (descriptor !is SimpleFunctionDescriptor) {
            return false
        }
        // TODO: receivers are currently not inline (KT-5837)
        // if (InlineUtil.isInline(descriptor)) return true

        val containingDeclaration = descriptor.getContainingDeclaration()
        return descriptor.getName() == OperatorNameConventions.INVOKE &&
                containingDeclaration is ClassDescriptor && containingDeclaration.defaultType.isBuiltinFunctionalType
    }

    private fun checkVisibilityAndAccess(
        calledDescriptor: CallableDescriptor,
        expression: KtElement,
        replacementForReport: KtElement?,
        context: CallCheckerContext,
        call: Call
    ) {
        val calledFunEffectiveVisibility = if (isDefinedInInlineFunction(calledDescriptor))
            EffectiveVisibility.Public
        else
            calledDescriptor.effectiveVisibility(calledDescriptor.visibility, true)

        val isCalledFunPublicOrPublishedApi = calledFunEffectiveVisibility.publicApi
        val isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.publicApi
        if (isInlineFunPublicOrPublishedApi &&
            !isCalledFunPublicOrPublishedApi &&
            calledDescriptor.visibility !== DescriptorVisibilities.LOCAL
        ) {
            context.trace.report(NON_PUBLIC_CALL_FROM_PUBLIC_INLINE.on(expression, calledDescriptor, descriptor))
            context.reportDeprecationOnReplacement(expression, replacementForReport)
        } else {
            checkPrivateClassMemberAccess(calledDescriptor, expression, replacementForReport, context)
            if (isInlineFunPublicOrPublishedApi) {
                checkSuperCalls(calledDescriptor, call, expression, context)
            }
        }

        val isConstructorCall = calledDescriptor is ConstructorDescriptor
        if ((!isConstructorCall || expression !is KtConstructorCalleeExpression) &&
            isInlineFunPublicOrPublishedApi &&
            inlineFunEffectiveVisibility.toVisibility() !== Visibilities.Protected &&
            calledFunEffectiveVisibility.toVisibility() === Visibilities.Protected
        ) {
            when {
                isConstructorCall -> {
                    context.trace.report(
                        PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE.on(context.languageVersionSettings, expression, calledDescriptor)
                    )
                }
                else -> {
                    context.trace.report(
                        PROTECTED_CALL_FROM_PUBLIC_INLINE.on(context.languageVersionSettings, expression, calledDescriptor)
                    )
                }
            }
            context.reportDeprecationOnReplacement(expression, replacementForReport)
        }
    }

    private fun checkPrivateClassMemberAccess(
        declarationDescriptor: DeclarationDescriptor,
        expression: KtElement,
        replacementForReport: KtElement?,
        context: CallCheckerContext
    ) {
        if (!isEffectivelyPrivateApiFunction) {
            if (declarationDescriptor.isInsidePrivateClass) {
                context.trace.report(PRIVATE_CLASS_MEMBER_FROM_INLINE.on(expression, declarationDescriptor, descriptor))
                context.reportDeprecationOnReplacement(expression, replacementForReport)
            } else if (declarationDescriptor.isMemberOfCompanionOfPrivateClass) {
                context.trace.report(PRIVATE_CLASS_MEMBER_FROM_INLINE_WARNING.on(expression, declarationDescriptor, descriptor))
                context.reportDeprecationOnReplacement(expression, replacementForReport)
            }
        }
    }

    private fun checkSuperCalls(
        callableDescriptor: CallableDescriptor,
        call: Call,
        expression: KtElement,
        context: CallCheckerContext
    ) {
        val superCall = getSuperCallExpression(call)
        if (superCall != null) {
            val thisTypeForSuperCall: KotlinType =
                context.trace.get(
                    BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION,
                    superCall
                ) ?: return
            val descriptor = thisTypeForSuperCall.constructor.declarationDescriptor as? DeclarationDescriptorWithVisibility ?: return

            if (!isDefinedInInlineFunction(descriptor)) {
                context.trace.report(
                    SUPER_CALL_FROM_PUBLIC_INLINE.on(
                        context.languageVersionSettings, expression.parent.parent ?: superCall, callableDescriptor
                    )
                )
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

    private fun CallCheckerContext.reportDeprecationOnReplacement(
        expression: KtElement,
        replacementForReport: KtElement?
    ) {
        if (!expression.isPhysical && replacementForReport != null) {
            trace.report(DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS.on(replacementForReport))
        }
    }
}
