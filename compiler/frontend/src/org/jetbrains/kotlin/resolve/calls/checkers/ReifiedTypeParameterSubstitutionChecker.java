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

package org.jetbrains.kotlin.resolve.calls.checkers;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.Map;

public class ReifiedTypeParameterSubstitutionChecker implements CallChecker {
    @Override
    public <F extends CallableDescriptor> void check(
            @NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context
    ) {
        Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();
        for (Map.Entry<TypeParameterDescriptor, KotlinType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor parameter = entry.getKey();
            KotlinType argument = entry.getValue();
            ClassifierDescriptor argumentDeclarationDescription = argument.getConstructor().getDeclarationDescriptor();

            if (parameter.isReified()) {
                if (argumentDeclarationDescription instanceof TypeParameterDescriptor &&
                    !((TypeParameterDescriptor) argumentDeclarationDescription).isReified()
                ) {
                    context.trace.report(
                            Errors.TYPE_PARAMETER_AS_REIFIED.on(getElementToReport(context, parameter.getIndex()), parameter)
                    );
                }
                else if (TypeUtilsKt.cannotBeReified(argument)) {
                    context.trace.report(
                            Errors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION.on(getElementToReport(context, parameter.getIndex()), argument));
                }
                else if (TypeUtilsKt.unsafeAsReifiedArgument(argument)) {
                    context.trace.report(
                            Errors.REIFIED_TYPE_UNSAFE_SUBSTITUTION.on(getElementToReport(context, parameter.getIndex()), argument));
                }
            }
        }
    }

    @NotNull
    private static PsiElement getElementToReport(@NotNull BasicCallResolutionContext context, int parameterIndex) {
        if (context.call.getTypeArguments().size() > parameterIndex) {
            return context.call.getTypeArguments().get(parameterIndex);
        }
        KtExpression callee = context.call.getCalleeExpression();
        return callee != null ? callee : context.call.getCallElement();
    }
}
