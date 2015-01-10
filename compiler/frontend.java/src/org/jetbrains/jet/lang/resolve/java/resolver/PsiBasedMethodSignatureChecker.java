/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesUtil;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaMethodImpl;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.SubstitutionUtils;
import org.jetbrains.kotlin.types.TypeSubstitution;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PsiBasedMethodSignatureChecker implements MethodSignatureChecker {
    private static final Logger LOG = Logger.getInstance(PsiBasedMethodSignatureChecker.class);

    private ExternalAnnotationResolver externalAnnotationResolver;
    private ExternalSignatureResolver externalSignatureResolver;

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    private void checkFunctionOverridesCorrectly(
            @NotNull JavaMethod method,
            @NotNull FunctionDescriptor function,
            @NotNull FunctionDescriptor superFunction
    ) {
        ClassDescriptor klass = (ClassDescriptor) function.getContainingDeclaration();
        List<TypeSubstitution> substitutions = new ArrayList<TypeSubstitution>();
        while (true) {
            substitutions.add(SubstitutionUtils.buildDeepSubstitutor(klass.getDefaultType()).getSubstitution());
            if (!klass.isInner()) {
                break;
            }
            klass = (ClassDescriptor) klass.getContainingDeclaration();
        }
        TypeSubstitutor substitutor = TypeSubstitutor.create(substitutions.toArray(new TypeSubstitution[substitutions.size()]));
        FunctionDescriptor superFunctionSubstituted = superFunction.substitute(substitutor);

        assert superFunctionSubstituted != null : "Couldn't substitute super function: " + superFunction + ", substitutor = " + substitutor;

        OverridingUtil.OverrideCompatibilityInfo.Result overridableResult = OverridingUtil.DEFAULT.isOverridableBy(superFunctionSubstituted, function).getResult();
        boolean paramsOk = overridableResult == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE;
        boolean returnTypeOk = OverrideResolver.isReturnTypeOkForOverride(superFunctionSubstituted, function);
        if (!paramsOk || !returnTypeOk) {
            // This should be a LOG.error, but happens a lot of times incorrectly (e.g. on Kotlin project), because somewhere in the
            // type checker we compare two types which seem the same but have different instances of class descriptors. It happens
            // probably because JavaDescriptorResolver is not completely thread-safe yet, and one class gets resolved multiple times.
            // TODO: change to LOG.error when JavaDescriptorResolver becomes thread-safe
            LOG.warn("Loaded Java method overrides another, but resolved as Kotlin function, doesn't.\n"
                      + "super function = " + superFunction + "\n"
                      + "super class = " + superFunction.getContainingDeclaration() + "\n"
                      + "sub function = " + function + "\n"
                      + "sub class = " + function.getContainingDeclaration() + "\n"
                      + "sub method = " + method + "\n"
                      + "@KotlinSignature = " + SignaturesUtil.getKotlinSignature(externalAnnotationResolver, method));
        }
    }

    private static boolean containsErrorType(@NotNull List<FunctionDescriptor> superFunctions, @NotNull FunctionDescriptor function) {
        if (ErrorUtils.containsErrorType(function)) {
            return true;
        }

        for (FunctionDescriptor superFunction : superFunctions) {
            if (ErrorUtils.containsErrorType(superFunction)) {
                return true;
            }
        }

        return false;
    }

    // Originally from com.intellij.codeInsight.daemon.impl.analysis.HighlightMethodUtil
    private static boolean isMethodReturnTypeCompatible(@NotNull JavaMethodImpl method) {
        if (method.isStatic()) return true;

        HierarchicalMethodSignature methodSignature = method.getPsi().getHierarchicalMethodSignature();
        List<HierarchicalMethodSignature> superSignatures = methodSignature.getSuperSignatures();

        PsiType returnType = methodSignature.getSubstitutor().substitute(method.getPsi().getReturnType());
        if (returnType == null) return true;

        for (MethodSignatureBackedByPsiMethod superMethodSignature : superSignatures) {
            PsiMethod superMethod = superMethodSignature.getMethod();
            PsiType declaredReturnType = superMethod.getReturnType();
            PsiType superReturnType = superMethodSignature.isRaw() ? TypeConversionUtil.erasure(declaredReturnType) : declaredReturnType;
            if (superReturnType == null || method == superMethod || superMethod.getContainingClass() == null) continue;
            if (!areMethodsReturnTypesCompatible(superMethodSignature, superReturnType, methodSignature, returnType)) {
                return false;
            }
        }

        return true;
    }

    // Originally from com.intellij.codeInsight.daemon.impl.analysis.HighlightMethodUtil
    private static boolean areMethodsReturnTypesCompatible(
            @NotNull MethodSignatureBackedByPsiMethod superMethodSignature,
            @NotNull PsiType superReturnType,
            @NotNull MethodSignatureBackedByPsiMethod methodSignature,
            @NotNull PsiType returnType
    ) {
        PsiType substitutedSuperReturnType;
        boolean isJdk15 = PsiUtil.isLanguageLevel5OrHigher(methodSignature.getMethod());
        if (isJdk15 && !superMethodSignature.isRaw() && superMethodSignature.equals(methodSignature)) { //see 8.4.5
            PsiSubstitutor unifyingSubstitutor = MethodSignatureUtil.getSuperMethodSignatureSubstitutor(methodSignature,
                                                                                                        superMethodSignature);
            substitutedSuperReturnType = unifyingSubstitutor == null
                                         ? superReturnType
                                         : unifyingSubstitutor.substitute(superReturnType);
        }
        else {
            substitutedSuperReturnType = TypeConversionUtil.erasure(superMethodSignature.getSubstitutor().substitute(superReturnType));
        }

        if (returnType.equals(substitutedSuperReturnType)) return true;
        if (!(returnType instanceof PsiPrimitiveType) && substitutedSuperReturnType.getDeepComponentType() instanceof PsiClassType) {
            if (isJdk15 && TypeConversionUtil.isAssignable(substitutedSuperReturnType, returnType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void checkSignature(
            @NotNull JavaMethod method,
            boolean reportSignatureErrors,
            @NotNull SimpleFunctionDescriptor descriptor,
            @NotNull List<String> signatureErrors,
            @NotNull List<FunctionDescriptor> superFunctions
    ) {
        // This optimization speed things up because hasRawTypesInHierarchicalSignature() is very expensive
        if (superFunctions.isEmpty() && (signatureErrors.isEmpty() || !reportSignatureErrors)) return;

        JavaMethodImpl methodWithPsi = (JavaMethodImpl) method;
        if (!RawTypesCheck.hasRawTypesInHierarchicalSignature(methodWithPsi) &&
            isMethodReturnTypeCompatible(methodWithPsi) &&
            !containsErrorType(superFunctions, descriptor)) {
            if (signatureErrors.isEmpty()) {
                for (FunctionDescriptor superFunction : superFunctions) {
                    checkFunctionOverridesCorrectly(method, descriptor, superFunction);
                }
            }
            else if (reportSignatureErrors) {
                externalSignatureResolver.reportSignatureErrors(descriptor, signatureErrors);
            }
        }
    }

    private static class RawTypesCheck {
        private static boolean isPartiallyRawType(@NotNull JavaType type) {
            if (type instanceof JavaPrimitiveType) {
                return false;
            }
            else if (type instanceof JavaClassifierType) {
                JavaClassifierType classifierType = (JavaClassifierType) type;

                if (classifierType.isRaw()) {
                    return true;
                }

                for (JavaType argument : classifierType.getTypeArguments()) {
                    if (isPartiallyRawType(argument)) {
                        return true;
                    }
                }

                return false;
            }
            else if (type instanceof JavaArrayType) {
                return isPartiallyRawType(((JavaArrayType) type).getComponentType());
            }
            else if (type instanceof JavaWildcardType) {
                JavaType bound = ((JavaWildcardType) type).getBound();
                return bound != null && isPartiallyRawType(bound);
            }
            else {
                throw new IllegalStateException("Unexpected type: " + type);
            }
        }

        private static boolean hasRawTypesInSignature(@NotNull JavaMethod method) {
            JavaType returnType = method.getReturnType();
            if (returnType != null && isPartiallyRawType(returnType)) {
                return true;
            }

            for (JavaValueParameter parameter : method.getValueParameters()) {
                if (isPartiallyRawType(parameter.getType())) {
                    return true;
                }
            }

            for (JavaTypeParameter typeParameter : method.getTypeParameters()) {
                for (JavaClassifierType upperBound : typeParameter.getUpperBounds()) {
                    if (isPartiallyRawType(upperBound)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public static boolean hasRawTypesInHierarchicalSignature(@NotNull JavaMethodImpl method) {
            // This is a very important optimization: package-classes are big and full of static methods
            // building method hierarchies for such classes takes a very long time
            if (method.isStatic()) return false;

            if (hasRawTypesInSignature(method)) {
                return true;
            }

            for (HierarchicalMethodSignature superSignature : method.getPsi().getHierarchicalMethodSignature().getSuperSignatures()) {
                JavaMethod superMethod = new JavaMethodImpl(superSignature.getMethod());
                if (superSignature.isRaw() || typeParameterIsErased(method, superMethod) || hasRawTypesInSignature(superMethod)) {
                    return true;
                }
            }

            return false;
        }

        private static boolean typeParameterIsErased(@NotNull JavaMethod method, @NotNull JavaMethod superMethod) {
            // Java allows you to write
            //   <T extends Foo> T foo(), in the superclass and then
            //   Foo foo(), in the subclass
            // this is a valid Java override, but in fact it is an erasure
            return method.getTypeParameters().size() != superMethod.getTypeParameters().size();
        }

        private RawTypesCheck() {
        }
    }
}
