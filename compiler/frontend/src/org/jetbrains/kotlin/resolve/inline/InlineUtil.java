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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMapping;
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.EnumValue;
import org.jetbrains.kotlin.types.JetType;

import static kotlin.KotlinPackage.firstOrNull;

public class InlineUtil {
    public static boolean isInlineLambdaParameter(@NotNull CallableDescriptor valueParameterOrReceiver) {
        JetType type = valueParameterOrReceiver.getOriginal().getReturnType();
        return type != null &&
               !KotlinBuiltIns.isNoinline(valueParameterOrReceiver) &&
               KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type);
    }

    public static boolean isInline(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof SimpleFunctionDescriptor && getInlineStrategy(descriptor).isInline();
    }

    @NotNull
    public static InlineStrategy getInlineStrategy(@NotNull DeclarationDescriptor descriptor) {
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.inline);
        if (annotation == null) {
            return InlineStrategy.NOT_INLINE;
        }
        CompileTimeConstant<?> argument = firstOrNull(annotation.getAllValueArguments().values());
        if (argument == null) {
            return InlineStrategy.AS_FUNCTION;
        }
        assert argument instanceof EnumValue : "Inline annotation parameter should be enum entry but was: " + argument;
        return InlineStrategy.valueOf(((EnumValue) argument).getValue().getName().asString());
    }

    public static boolean hasOnlyLocalContinueAndBreak(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.LOCAL_CONTINUE_AND_BREAK);
    }

    public static boolean hasOnlyLocalReturn(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.ONLY_LOCAL_RETURN);
    }

    private static boolean hasInlineOption(@NotNull ValueParameterDescriptor descriptor, @NotNull InlineOption option) {
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(
                DescriptorUtils.getFqNameSafe(KotlinBuiltIns.getInstance().getInlineOptionsClassAnnotation())
        );
        if (annotation != null) {
            CompileTimeConstant<?> argument = firstOrNull(annotation.getAllValueArguments().values());
            if (argument instanceof ArrayValue) {
                for (CompileTimeConstant<?> value : ((ArrayValue) argument).getValue()) {
                    if (value instanceof EnumValue && ((EnumValue) value).getValue().getName().asString().equals(option.name())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean checkNonLocalReturnUsage(
            @NotNull DeclarationDescriptor fromFunction,
            @NotNull JetExpression startExpression,
            @NotNull BindingTrace trace
    ) {
        PsiElement containingFunction = PsiTreeUtil.getParentOfType(startExpression, JetClassOrObject.class, JetDeclarationWithBody.class);
        if (containingFunction == null) {
            return false;
        }

        DeclarationDescriptor containingFunctionDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, containingFunction);
        if (containingFunctionDescriptor == null) {
            return false;
        }

        BindingContext bindingContext = trace.getBindingContext();

        while (containingFunction instanceof JetFunctionLiteral && fromFunction != containingFunctionDescriptor) {
            //JetFunctionLiteralExpression
            containingFunction = containingFunction.getParent();
            if (!isInlineLambda((JetFunctionLiteralExpression) containingFunction, bindingContext, true)) {
                return false;
            }

            containingFunctionDescriptor = getContainingClassOrFunctionDescriptor(containingFunctionDescriptor, true);

            containingFunction = containingFunctionDescriptor != null
                                 ? DescriptorToSourceUtils.descriptorToDeclaration(containingFunctionDescriptor)
                                 : null;
        }

        return fromFunction == containingFunctionDescriptor;
    }

    public static boolean isInlineLambda(
            @NotNull JetFunctionLiteralExpression lambdaExpression,
            @NotNull BindingContext bindingContext,
            boolean checkNonLocalReturn
    ) {
        JetExpression call = JetPsiUtil.getParentCallIfPresent(lambdaExpression);
        if (call != null) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(call, bindingContext);
            if (resolvedCall != null && isInline(resolvedCall.getResultingDescriptor())) {
                ValueArgument argument = CallUtilPackage.getValueArgumentForExpression(resolvedCall.getCall(), lambdaExpression);
                if (argument != null) {
                    ArgumentMapping mapping = resolvedCall.getArgumentMapping(argument);
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
            if (hasOnlyLocalReturn((ValueParameterDescriptor) lambda)) {
                //annotated
                return false;
            }
        }
        return true;
    }
}
