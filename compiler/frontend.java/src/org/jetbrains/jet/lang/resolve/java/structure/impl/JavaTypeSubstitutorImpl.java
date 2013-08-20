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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.PsiSubstitutorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class JavaTypeSubstitutorImpl implements JavaTypeSubstitutor {
    private final PsiSubstitutor psiSubstitutor;
    private Map<JavaTypeParameter, JavaType> substitutionMap;

    public JavaTypeSubstitutorImpl(@NotNull PsiSubstitutor psiSubstitutor) {
        this.psiSubstitutor = psiSubstitutor;
    }

    public JavaTypeSubstitutorImpl(@NotNull PsiSubstitutor psiSubstitutor, @NotNull Map<JavaTypeParameter, JavaType> substitutionMap) {
        this(psiSubstitutor);
        this.substitutionMap = substitutionMap;
    }

    @NotNull
    public static JavaTypeSubstitutor create(@NotNull Map<JavaTypeParameter, JavaType> substitutionMap) {
        Map<PsiTypeParameter, PsiType> psiMap = new HashMap<PsiTypeParameter, PsiType>();
        for (Map.Entry<JavaTypeParameter, JavaType> entry : substitutionMap.entrySet()) {
            JavaTypeImpl value = ((JavaTypeImpl) entry.getValue());
            psiMap.put(((JavaTypeParameterImpl) entry.getKey()).getPsi(), value == null ? null : value.getPsi());
        }
        PsiSubstitutor psiSubstitutor = PsiSubstitutorImpl.createSubstitutor(psiMap);
        return new JavaTypeSubstitutorImpl(psiSubstitutor, substitutionMap);
    }

    @Override
    @NotNull
    public JavaType substitute(@NotNull JavaType type) {
        return JavaTypeImpl.create(psiSubstitutor.substitute(((JavaTypeImpl) type).getPsi()));
    }

    @Override
    @Nullable
    public JavaType substitute(@NotNull JavaTypeParameter typeParameter) {
        PsiType psiType = psiSubstitutor.substitute(((JavaTypeParameterImpl) typeParameter).getPsi());
        return psiType == null ? null : JavaTypeImpl.create(psiType);
    }

    @Override
    @NotNull
    public Map<JavaTypeParameter, JavaType> getSubstitutionMap() {
        if (substitutionMap == null) {
            Map<PsiTypeParameter, PsiType> psiMap = psiSubstitutor.getSubstitutionMap();
            substitutionMap = new HashMap<JavaTypeParameter, JavaType>();
            for (Map.Entry<PsiTypeParameter, PsiType> entry : psiMap.entrySet()) {
                PsiType value = entry.getValue();
                substitutionMap.put(new JavaTypeParameterImpl(entry.getKey()), value == null ? null : JavaTypeImpl.create(value));
            }
        }

        return substitutionMap;
    }

    @Override
    public int hashCode() {
        return psiSubstitutor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaTypeSubstitutorImpl && psiSubstitutor.equals(((JavaTypeSubstitutorImpl) obj).psiSubstitutor);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + psiSubstitutor;
    }
}
