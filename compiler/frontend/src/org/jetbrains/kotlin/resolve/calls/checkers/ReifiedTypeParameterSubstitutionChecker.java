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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeatureSettings;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.Map;

public class ReifiedTypeParameterSubstitutionChecker implements SimpleCallChecker {
    @Override
    public void check(
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull BasicCallResolutionContext context,
            @NotNull LanguageFeatureSettings languageFeatureSettings
    ) {
        SimpleCallChecker.DefaultImpls.check(this, resolvedCall, context, languageFeatureSettings);
    }

    @Override
    public void check(@NotNull ResolvedCall<?> resolvedCall, @NotNull BasicCallResolutionContext context) {
        Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();
        for (Map.Entry<TypeParameterDescriptor, KotlinType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor parameter = entry.getKey();
            KotlinType argument = entry.getValue();
            ClassifierDescriptor argumentDeclarationDescriptor = argument.getConstructor().getDeclarationDescriptor();

            if (!parameter.isReified() && !isTypeParameterOfKotlinArray(parameter)) {
                continue;
            }

            if (argumentDeclarationDescriptor instanceof TypeParameterDescriptor &&
                !((TypeParameterDescriptor) argumentDeclarationDescriptor).isReified()) {
                context.trace.report(
                        Errors.TYPE_PARAMETER_AS_REIFIED.on(getElementToReport(context, parameter.getIndex()), parameter)
                );
            }
            else if (TypeUtilsKt.cannotBeReified(argument)) {
                context.trace.report(
                        Errors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION.on(getElementToReport(context, parameter.getIndex()), argument));
            }
            // REIFIED_TYPE_UNSAFE_SUBSTITUTION is temporary disabled because it seems too strict now (see KT-10847)
            //else if (TypeUtilsKt.unsafeAsReifiedArgument(argument) && !hasPureReifiableAnnotation(parameter)) {
            //    context.trace.report(
            //            Errors.REIFIED_TYPE_UNSAFE_SUBSTITUTION.on(getElementToReport(context, parameter.getIndex()), argument));
            //}
        }
    }

    private static final FqName PURE_REIFIABLE_ANNOTATION_FQ_NAME = new FqName("kotlin.internal.PureReifiable");

    private static boolean hasPureReifiableAnnotation(@NotNull TypeParameterDescriptor parameter) {
        return parameter.getAnnotations().hasAnnotation(PURE_REIFIABLE_ANNOTATION_FQ_NAME) ||
               isTypeParameterOfKotlinArray(parameter);
    }

    private static boolean isTypeParameterOfKotlinArray(@NotNull TypeParameterDescriptor parameter) {
        DeclarationDescriptor container = parameter.getContainingDeclaration();
        return container instanceof ClassDescriptor && KotlinBuiltIns.isNonPrimitiveArray((ClassDescriptor) container);
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
