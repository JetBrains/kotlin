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

package org.jetbrains.kotlin.resolve.calls.checkers;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.NON_LOCAL_RETURN_NOT_ALLOWED;
import static org.jetbrains.kotlin.diagnostics.Errors.USAGE_IS_NOT_INLINABLE;
import static org.jetbrains.kotlin.resolve.inline.InlineUtil.allowsNonLocalReturns;
import static org.jetbrains.kotlin.resolve.inline.InlineUtil.checkNonLocalReturnUsage;

class InlineChecker implements CallChecker {
    private final FunctionDescriptor descriptor;
    private final Set<CallableDescriptor> inlinableParameters = new LinkedHashSet<CallableDescriptor>();
    private final EffectiveVisibility inlineFunEffectiveVisibility;
    private final boolean isEffectivelyPrivateApiFunction;

    public InlineChecker(@NotNull FunctionDescriptor descriptor) {
        assert InlineUtil.isInline(descriptor) : "This extension should be created only for inline functions: " + descriptor;
        this.descriptor = descriptor;
        this.inlineFunEffectiveVisibility = EffectiveVisibilityKt.effectiveVisibility(descriptor, descriptor.getVisibility(), true);
        this.isEffectivelyPrivateApiFunction = DescriptorUtilsKt.isEffectivelyPrivateApi(descriptor);
        for (ValueParameterDescriptor param : descriptor.getValueParameters()) {
            if (isInlinableParameter(param)) {
                inlinableParameters.add(param);
            }
        }
    }

    @Override
    public void check(@NotNull ResolvedCall<?> resolvedCall, @NotNull PsiElement reportOn, @NotNull CallCheckerContext context) {
        KtExpression expression = resolvedCall.getCall().getCalleeExpression();
        if (expression == null) {
            return;
        }

        //checking that only invoke or inlinable extension called on function parameter
        CallableDescriptor targetDescriptor = resolvedCall.getResultingDescriptor();
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getDispatchReceiver(), expression);
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getExtensionReceiver(), expression);

        if (inlinableParameters.contains(targetDescriptor)) {
            if (!isInsideCall(expression)) {
                context.getTrace().report(USAGE_IS_NOT_INLINABLE.on(expression, expression, descriptor));
            }
        }

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument value = entry.getValue();
            ValueParameterDescriptor valueDescriptor = entry.getKey();
            if (!(value instanceof DefaultValueArgument)) {
                for (ValueArgument argument : value.getArguments()) {
                    checkValueParameter(context, targetDescriptor, argument, valueDescriptor);
                }
            }
        }

        checkVisibilityAndAccess(targetDescriptor, expression, context);
        checkRecursion(context, targetDescriptor, expression);
    }

    private static boolean isInsideCall(KtExpression expression) {
        KtElement parent = KtPsiUtil.getParentCallIfPresent(expression);
        if (parent instanceof KtBinaryExpression) {
            KtToken token = KtPsiUtil.getOperationToken((KtOperationExpression) parent);
            if (token == KtTokens.EQ || token == KtTokens.ANDAND || token == KtTokens.OROR) {
                //assignment
                return false;
            }
        }

        if (parent != null) {
            //UGLY HACK
            //check there is no casts
            PsiElement current = expression;
            while (current != parent) {
                if (current instanceof KtBinaryExpressionWithTypeRHS) {
                    return false;
                }
                current = current.getParent();
            }
        }

        return parent != null;
    }

    private void checkValueParameter(
            @NotNull CallCheckerContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull ValueArgument targetArgument,
            @NotNull ValueParameterDescriptor targetParameterDescriptor
    ) {
        KtExpression argumentExpression = targetArgument.getArgumentExpression();
        if (argumentExpression == null) {
            return;
        }
        CallableDescriptor argumentCallee = getCalleeDescriptor(context, argumentExpression, false);

        if (argumentCallee != null && inlinableParameters.contains(argumentCallee)) {
            if (InlineUtil.isInline(targetDescriptor) && isInlinableParameter(targetParameterDescriptor)) {
                if (allowsNonLocalReturns(argumentCallee) && !allowsNonLocalReturns(targetParameterDescriptor)) {
                    context.getTrace().report(NON_LOCAL_RETURN_NOT_ALLOWED.on(argumentExpression, argumentExpression));
                }
                else {
                    checkNonLocalReturn(context, argumentCallee, argumentExpression);
                }
            }
            else {
                context.getTrace().report(USAGE_IS_NOT_INLINABLE.on(argumentExpression, argumentExpression, descriptor));
            }
        }
    }

    private void checkCallWithReceiver(
            @NotNull CallCheckerContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @Nullable ReceiverValue receiver,
            @Nullable KtExpression expression
    ) {
        if (receiver == null) return;

        CallableDescriptor varDescriptor = null;
        KtExpression receiverExpression = null;
        if (receiver instanceof ExpressionReceiver) {
            receiverExpression = ((ExpressionReceiver) receiver).getExpression();
            varDescriptor = getCalleeDescriptor(context, receiverExpression, true);
        }
        else if (receiver instanceof ExtensionReceiver) {
            ExtensionReceiver extensionReceiver = (ExtensionReceiver) receiver;
            CallableDescriptor extension = extensionReceiver.getDeclarationDescriptor();

            varDescriptor = extension.getExtensionReceiverParameter();
            assert varDescriptor != null : "Extension should have receiverParameterDescriptor: " + extension;

            receiverExpression = expression;
        }

        if (inlinableParameters.contains(varDescriptor)) {
            //check that it's invoke or inlinable extension
            checkLambdaInvokeOrExtensionCall(context, varDescriptor, targetDescriptor, receiverExpression);
        }
    }

    @Nullable
    private static CallableDescriptor getCalleeDescriptor(
            @NotNull CallCheckerContext context,
            @NotNull KtExpression expression,
            boolean unwrapVariableAsFunction
    ) {
        if (!(expression instanceof KtSimpleNameExpression || expression instanceof KtThisExpression)) return null;

        ResolvedCall<?> thisCall = CallUtilKt.getResolvedCall(expression, context.getTrace().getBindingContext());
        if (unwrapVariableAsFunction && thisCall instanceof VariableAsFunctionResolvedCall) {
            return ((VariableAsFunctionResolvedCall) thisCall).getVariableCall().getResultingDescriptor();
        }
        return thisCall != null ? thisCall.getResultingDescriptor() : null;
    }

    private void checkLambdaInvokeOrExtensionCall(
            @NotNull CallCheckerContext context,
            @NotNull CallableDescriptor lambdaDescriptor,
            @NotNull CallableDescriptor callDescriptor,
            @NotNull KtExpression receiverExpression
    ) {
        boolean inlinableCall = isInvokeOrInlineExtension(callDescriptor);
        if (!inlinableCall) {
            context.getTrace().report(USAGE_IS_NOT_INLINABLE.on(receiverExpression, receiverExpression, descriptor));
        }
        else {
            checkNonLocalReturn(context, lambdaDescriptor, receiverExpression);
        }
    }

    private void checkRecursion(
            @NotNull CallCheckerContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull KtElement expression
    ) {
        if (targetDescriptor.getOriginal() == descriptor) {
            context.getTrace().report(Errors.RECURSION_IN_INLINE.on(expression, expression, descriptor));
        }
    }

    private static boolean isInlinableParameter(@NotNull ParameterDescriptor descriptor) {
        return InlineUtil.isInlineLambdaParameter(descriptor) && !descriptor.getType().isMarkedNullable();
    }

    private static boolean isInvokeOrInlineExtension(@NotNull CallableDescriptor descriptor) {
        if (!(descriptor instanceof SimpleFunctionDescriptor)) {
            return false;
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        boolean isInvoke =
                descriptor.getName().equals(OperatorNameConventions.INVOKE) &&
                containingDeclaration instanceof ClassDescriptor &&
                FunctionTypesKt.isFunctionType(((ClassDescriptor) containingDeclaration).getDefaultType());

        return isInvoke || InlineUtil.isInline(descriptor);
    }

    private void checkVisibilityAndAccess(
            @NotNull CallableDescriptor calledDescriptor,
            @NotNull KtElement expression,
            @NotNull CallCheckerContext context
    ) {
        EffectiveVisibility calledFunEffectiveVisibility =
                isDefinedInInlineFunction(calledDescriptor) ?
                EffectiveVisibility.Public.INSTANCE :
                EffectiveVisibilityKt.effectiveVisibility(calledDescriptor, calledDescriptor.getVisibility(), true);

        boolean isCalledFunPublicOrPublishedApi = calledFunEffectiveVisibility.getPublicApi();
        boolean isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.getPublicApi();
        if (isInlineFunPublicOrPublishedApi &&
            !isCalledFunPublicOrPublishedApi &&
            calledDescriptor.getVisibility() != Visibilities.LOCAL) {
            context.getTrace().report(Errors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE.on(expression, calledDescriptor, descriptor));
        }
        else {
            checkPrivateClassMemberAccess(calledDescriptor, expression, context);
        }

        if (!(calledDescriptor instanceof ConstructorDescriptor) &&
            isInlineFunPublicOrPublishedApi &&
            inlineFunEffectiveVisibility.toVisibility() != Visibilities.PROTECTED &&
            calledFunEffectiveVisibility.toVisibility() == Visibilities.PROTECTED) {
            context.getTrace().report(Errors.PROTECTED_CALL_FROM_PUBLIC_INLINE.on(expression, calledDescriptor));
        }
    }

    private void checkPrivateClassMemberAccess(
            @NotNull DeclarationDescriptor declarationDescriptor,
            @NotNull KtElement expression,
            @NotNull CallCheckerContext context
    ) {
        if (!isEffectivelyPrivateApiFunction) {
            if (DescriptorUtilsKt.isInsidePrivateClass(declarationDescriptor)) {
                context.getTrace().report(Errors.PRIVATE_CLASS_MEMBER_FROM_INLINE.on(expression, declarationDescriptor, descriptor));
            }
        }
    }

    private boolean isDefinedInInlineFunction(@NotNull DeclarationDescriptorWithVisibility startDescriptor) {
        DeclarationDescriptorWithVisibility parent = startDescriptor;

        while (parent != null) {
            if (parent.getContainingDeclaration() == descriptor) return true;

            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }

        return false;
    }

    private void checkNonLocalReturn(
            @NotNull CallCheckerContext context,
            @NotNull CallableDescriptor inlinableParameterDescriptor,
            @NotNull KtExpression parameterUsage
    ) {
        if (!allowsNonLocalReturns(inlinableParameterDescriptor)) return;

        if (!checkNonLocalReturnUsage(descriptor, parameterUsage, context.getResolutionContext())) {
            context.getTrace().report(NON_LOCAL_RETURN_NOT_ALLOWED.on(parameterUsage, parameterUsage));
        }
    }
}
