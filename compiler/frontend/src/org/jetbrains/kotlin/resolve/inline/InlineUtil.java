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

package org.jetbrains.kotlin.resolve.inline;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMapping;
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

public class InlineUtil {
    public static boolean isInlineLambdaParameter(@NotNull ParameterDescriptor valueParameterOrReceiver) {
        return !(valueParameterOrReceiver instanceof ValueParameterDescriptor
                 && ((ValueParameterDescriptor) valueParameterOrReceiver).isNoinline()) &&
               KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(valueParameterOrReceiver.getOriginal().getType());
    }

    public static boolean isInline(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof SimpleFunctionDescriptor && getInlineStrategy(descriptor).isInline();
    }

    @NotNull
    public static InlineStrategy getInlineStrategy(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor &&
            ((FunctionDescriptor) descriptor).isInline()) {
            return InlineStrategy.AS_FUNCTION;
        }

        return InlineStrategy.NOT_INLINE;
    }

    public static boolean checkNonLocalReturnUsage(
            @NotNull DeclarationDescriptor fromFunction,
            @NotNull KtExpression startExpression,
            @NotNull BindingTrace trace
    ) {
        PsiElement containingFunction = PsiTreeUtil.getParentOfType(startExpression, KtClassOrObject.class, KtDeclarationWithBody.class);
        if (containingFunction == null) {
            return false;
        }

        DeclarationDescriptor containingFunctionDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, containingFunction);
        if (containingFunctionDescriptor == null) {
            return false;
        }

        BindingContext bindingContext = trace.getBindingContext();

        while (canBeInlineArgument(containingFunction) && fromFunction != containingFunctionDescriptor) {
            if (!isInlinedArgument((KtFunction) containingFunction, bindingContext, true)) {
                return false;
            }

            containingFunctionDescriptor = getContainingClassOrFunctionDescriptor(containingFunctionDescriptor, true);

            containingFunction = containingFunctionDescriptor != null
                                 ? DescriptorToSourceUtils.descriptorToDeclaration(containingFunctionDescriptor)
                                 : null;
        }

        return fromFunction == containingFunctionDescriptor;
    }

    public static boolean isInlinedArgument(
            @NotNull KtFunction argument,
            @NotNull BindingContext bindingContext,
            boolean checkNonLocalReturn
    ) {
        if (!canBeInlineArgument(argument)) return false;

        KtExpression call = KtPsiUtil.getParentCallIfPresent(argument);
        if (call != null) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(call, bindingContext);
            if (resolvedCall != null && isInline(resolvedCall.getResultingDescriptor())) {
                ValueArgument valueArgument = CallUtilKt.getValueArgumentForExpression(resolvedCall.getCall(), argument);
                if (valueArgument != null) {
                    ArgumentMapping mapping = resolvedCall.getArgumentMapping(valueArgument);
                    if (mapping instanceof ArgumentMatch) {
                        ValueParameterDescriptor parameter = ((ArgumentMatch) mapping).getValueParameter();
                        if (isInlineLambdaParameter(parameter)) {
                            return !checkNonLocalReturn || allowsNonLocalReturns(parameter);
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean canBeInlineArgument(@Nullable PsiElement functionalExpression) {
        return functionalExpression instanceof KtFunctionLiteral || functionalExpression instanceof KtNamedFunction;
    }

    @Nullable
    public static DeclarationDescriptor getContainingClassOrFunctionDescriptor(@NotNull DeclarationDescriptor descriptor, boolean strict) {
        DeclarationDescriptor current = strict ? descriptor.getContainingDeclaration() : descriptor;
        while (current != null) {
            if (current instanceof FunctionDescriptor || current instanceof ClassDescriptor) {
                return current;
            }
            current = current.getContainingDeclaration();
        }

        return null;
    }

    public static boolean allowsNonLocalReturns(@NotNull CallableDescriptor lambda) {
        if (lambda instanceof ValueParameterDescriptor) {
            if (((ValueParameterDescriptor) lambda).isCrossinline()) {
                //annotated
                return false;
            }
        }
        return true;
    }
}
