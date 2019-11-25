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
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.Map;

public class ReifiedTypeParameterSubstitutionChecker implements CallChecker {
    @Override
    public void check(@NotNull ResolvedCall<?> resolvedCall, @NotNull PsiElement reportOn, @NotNull CallCheckerContext context) {
        Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();
        for (Map.Entry<TypeParameterDescriptor, KotlinType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor parameter = entry.getKey();
            KotlinType argument = entry.getValue();
            ClassifierDescriptor argumentDeclarationDescriptor = argument.getConstructor().getDeclarationDescriptor();

            if (!parameter.isReified() && !isTypeParameterOfKotlinArray(parameter)) {
                continue;
            }

            KtTypeProjection typeProjection = CollectionsKt.getOrNull(resolvedCall.getCall().getTypeArguments(), parameter.getIndex());

            checkTypeArgument(typeProjection != null ? typeProjection : reportOn, context, argument, argumentDeclarationDescriptor, false);
        }
    }

    private void checkTypeArgument(
            @NotNull PsiElement reportErrorOn,
            @NotNull CallCheckerContext context,
            KotlinType argument,
            ClassifierDescriptor argumentDeclarationDescriptor,
            boolean isArrayArgumentCheck
    ) {
        if (argumentDeclarationDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) argumentDeclarationDescriptor;
            if (typeParameter.isReified()) return;

            DiagnosticFactory1<PsiElement, TypeParameterDescriptor> diagnosticFactory;

            if (isArrayArgumentCheck) {
                if (context.getLanguageVersionSettings().supportsFeature(LanguageFeature.ProhibitNonReifiedArraysAsReifiedTypeArguments)) {
                    diagnosticFactory = Errors.TYPE_PARAMETER_AS_REIFIED_ARRAY;
                } else {
                    diagnosticFactory = Errors.TYPE_PARAMETER_AS_REIFIED_ARRAY_WARNING;
                }
            } else {
                diagnosticFactory = Errors.TYPE_PARAMETER_AS_REIFIED;
            }
            context.getTrace().report(diagnosticFactory.on(reportErrorOn, typeParameter));
        }
        else if (TypeUtilsKt.cannotBeReified(argument)) {
            context.getTrace().report(Errors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION.on(reportErrorOn, argument));
        }
        else if (argumentDeclarationDescriptor instanceof ClassDescriptor &&
                 KotlinBuiltIns.isNonPrimitiveArray((ClassDescriptor) argumentDeclarationDescriptor)) {
            KotlinType arrayTypeArgument = argument.getArguments().get(0).getType();
            ClassifierDescriptor arrayTypeArgumentDescriptor = arrayTypeArgument.getConstructor().getDeclarationDescriptor();
            checkTypeArgument(reportErrorOn, context, arrayTypeArgument, arrayTypeArgumentDescriptor, true);
        }
        // REIFIED_TYPE_UNSAFE_SUBSTITUTION is temporary disabled because it seems too strict now (see KT-10847)
        //else if (TypeUtilsKt.unsafeAsReifiedArgument(argument) && !hasPureReifiableAnnotation(parameter)) {
        //    context.getTrace().report(Errors.REIFIED_TYPE_UNSAFE_SUBSTITUTION.on(reportErrorOn, argument));
        //}
    }

    /*
    private static final FqName PURE_REIFIABLE_ANNOTATION_FQ_NAME = new FqName("kotlin.internal.PureReifiable");

    private static boolean hasPureReifiableAnnotation(@NotNull TypeParameterDescriptor parameter) {
        return parameter.getAnnotations().hasAnnotation(PURE_REIFIABLE_ANNOTATION_FQ_NAME) ||
               isTypeParameterOfKotlinArray(parameter);
    }
    */

    private static boolean isTypeParameterOfKotlinArray(@NotNull TypeParameterDescriptor parameter) {
        DeclarationDescriptor container = parameter.getContainingDeclaration();
        return container instanceof ClassDescriptor && KotlinBuiltIns.isNonPrimitiveArray((ClassDescriptor) container);
    }
}
