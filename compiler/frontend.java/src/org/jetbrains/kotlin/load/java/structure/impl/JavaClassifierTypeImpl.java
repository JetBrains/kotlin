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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaClassifierTypeImpl extends JavaTypeImpl<PsiClassType> implements JavaClassifierType {
    private static class ResolutionResult {
        private final JavaClassifierImpl<?> classifier;
        private final PsiSubstitutor substitutor;
        private final boolean isRaw;

        private ResolutionResult(@Nullable JavaClassifierImpl<?> classifier, @NotNull PsiSubstitutor substitutor, boolean isRaw) {
            this.classifier = classifier;
            this.substitutor = substitutor;
            this.isRaw = isRaw;
        }
    }

    private ResolutionResult resolutionResult;

    public JavaClassifierTypeImpl(@NotNull PsiClassType psiClassType) {
        super(psiClassType);
    }

    @Override
    @Nullable
    public JavaClassifierImpl<?> getClassifier() {
        resolve();
        return resolutionResult.classifier;
    }

    @NotNull
    public PsiSubstitutor getSubstitutor() {
        resolve();
        return resolutionResult.substitutor;
    }

    private void resolve() {
        if (resolutionResult == null) {
            PsiClassType.ClassResolveResult result = getPsi().resolveGenerics();
            PsiClass psiClass = result.getElement();
            PsiSubstitutor substitutor = result.getSubstitutor();
            resolutionResult = new ResolutionResult(
                    psiClass == null ? null : JavaClassifierImpl.create(psiClass), substitutor, PsiClassType.isRaw(result)
            );
        }
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        return getPsi().getCanonicalText();
    }

    @Override
    @NotNull
    public String getPresentableText() {
        return getPsi().getPresentableText();
    }

    @Override
    public boolean isRaw() {
        resolve();
        return resolutionResult.isRaw;
    }

    @Override
    @NotNull
    public List<JavaType> getTypeArguments() {
        JavaClassifierImpl<?> classifier = getClassifier();
        if (!(classifier instanceof JavaClassImpl)) return Collections.emptyList();

        // parameters including ones from outer class
        List<PsiTypeParameter> parameters = getTypeParameters(classifier.getPsi());

        PsiSubstitutor substitutor = getSubstitutor();

        List<JavaType> result = new ArrayList<JavaType>(parameters.size());
        for (PsiTypeParameter typeParameter : parameters) {
            PsiType substitutedType = substitutor.substitute(typeParameter);
            result.add(substitutedType == null ? null : JavaTypeImpl.create(substitutedType));
        }

        return result;
    }

    // Copy-pasted from PsiUtil.typeParametersIterable
    // The only change is using `Collections.addAll(result, typeParameters)` instead of reversing type parameters of `currentOwner`
    // Result differs in cases like:
    // class Outer<H1> {
    //   class Inner<H2, H3> {}
    // }
    //
    // PsiUtil.typeParametersIterable returns H3, H2, H1
    // But we would like to have H2, H3, H1 as such order is consistent with our type representation
    @NotNull
    private static List<PsiTypeParameter> getTypeParameters(@NotNull PsiClass owner) {
        List<PsiTypeParameter> result = null;

        PsiTypeParameterListOwner currentOwner = owner;
        while (currentOwner != null) {
            PsiTypeParameter[] typeParameters = currentOwner.getTypeParameters();
            if (typeParameters.length > 0) {
                if (result == null) result = new ArrayList<PsiTypeParameter>(typeParameters.length);
                Collections.addAll(result, typeParameters);
            }

            if (currentOwner.hasModifierProperty(PsiModifier.STATIC)) break;
            currentOwner = currentOwner.getContainingClass();
        }

        if (result == null) return Collections.emptyList();
        return result;
    }
}
