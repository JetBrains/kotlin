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
import org.jetbrains.kotlin.load.java.structure.*;

import java.util.*;

public class JavaClassifierTypeImpl extends JavaTypeImpl<PsiClassType> implements JavaClassifierType {
    private static class ResolutionResult {
        private final JavaClassifier classifier;
        private final JavaTypeSubstitutor substitutor;
        private final boolean isRaw;

        private ResolutionResult(@Nullable JavaClassifier classifier, @NotNull JavaTypeSubstitutor substitutor, boolean isRaw) {
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
    public JavaClassifier getClassifier() {
        resolve();
        return resolutionResult.classifier;
    }

    @Override
    @NotNull
    public JavaTypeSubstitutor getSubstitutor() {
        resolve();
        return resolutionResult.substitutor;
    }

    private void resolve() {
        if (resolutionResult == null) {
            PsiClassType.ClassResolveResult result = getPsi().resolveGenerics();
            PsiClass psiClass = result.getElement();
            PsiSubstitutor substitutor = result.getSubstitutor();
            resolutionResult = new ResolutionResult(
                    psiClass == null ? null : JavaClassifierImpl.create(psiClass),
                    new JavaTypeSubstitutorImpl(convertSubstitutionMap(substitutor.getSubstitutionMap())),
                    PsiClassType.isRaw(result));
        }
    }

    @NotNull
    private static Map<JavaTypeParameter, JavaType> convertSubstitutionMap(@NotNull Map<PsiTypeParameter, PsiType> psiMap) {
        if (psiMap.isEmpty()) return Collections.emptyMap();

        Map<JavaTypeParameter, JavaType> substitutionMap = new HashMap<JavaTypeParameter, JavaType>();
        for (Map.Entry<PsiTypeParameter, PsiType> entry : psiMap.entrySet()) {
            PsiType value = entry.getValue();
            substitutionMap.put(new JavaTypeParameterImpl(entry.getKey()), value == null ? null : JavaTypeImpl.create(value));
        }

        return substitutionMap;
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        PsiType[] psiTypes = getPsi().getSuperTypes();
        if (psiTypes.length == 0) return Collections.emptyList();
        List<JavaClassifierType> result = new ArrayList<JavaClassifierType>(psiTypes.length);
        for (PsiType psiType : psiTypes) {
            if (!(psiType instanceof PsiClassType)) {
                throw new IllegalStateException("Supertype should be a class: " + psiType + ", type: " + getPsi());
            }
            result.add(new JavaClassifierTypeImpl((PsiClassType) psiType));
        }
        return result;
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
        JavaClassifier classifier = getClassifier();

        // parameters including ones from outer class
        Iterable<PsiTypeParameter> parameters = classifier instanceof JavaClassImpl
                                                ? getTypeParameters(((JavaClassImpl) classifier).getPsi())
                                                : Collections.<PsiTypeParameter>emptyList();

        JavaTypeSubstitutor substitutor = getSubstitutor();

        List<JavaType> result = new ArrayList<JavaType>();
        for (PsiTypeParameter typeParameter : parameters) {
            result.add(substitutor.substitute(new JavaTypeParameterImpl(typeParameter)));
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
    public static List<PsiTypeParameter> getTypeParameters(@NotNull PsiClass owner) {
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
