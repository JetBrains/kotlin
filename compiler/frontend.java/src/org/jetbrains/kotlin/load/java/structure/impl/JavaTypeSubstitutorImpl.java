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
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassifier;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaTypeSubstitutorImpl {
    private final Map<JavaTypeParameterImpl, JavaTypeImpl<?>> substitutionMap;

    public JavaTypeSubstitutorImpl(@NotNull Map<JavaTypeParameterImpl, JavaTypeImpl<?>> substitutionMap) {
        this.substitutionMap = substitutionMap;
    }

    @NotNull
    public JavaTypeImpl<?> substitute(@NotNull JavaTypeImpl<?> type) {
        JavaTypeImpl<?> substitutedType = substituteInternal(type);
        return substitutedType != null ? substitutedType : correctSubstitutionForRawType(type);
    }

    @NotNull
    // In case of raw type we get substitution map like T -> null,
    // in this case we should substitute upper bound of T or,
    // if it does not exist, return java.lang.Object
    private JavaTypeImpl<?> correctSubstitutionForRawType(@NotNull JavaTypeImpl<?> original) {
        if (original instanceof JavaClassifierType) {
            JavaClassifier classifier = ((JavaClassifierType) original).getClassifier();
            if (classifier instanceof JavaTypeParameterImpl) {
                return rawTypeForTypeParameter((JavaTypeParameterImpl) classifier);
            }
        }

        return original;
    }

    @Nullable
    private JavaTypeImpl<?> substituteInternal(@NotNull JavaTypeImpl<?> type) {
        if (type instanceof JavaClassifierTypeImpl) {
            JavaClassifierTypeImpl classifierType = (JavaClassifierTypeImpl) type;
            JavaClassifierImpl<?> classifier = classifierType.getClassifier();

            if (classifier instanceof JavaTypeParameterImpl) {
                return substitute((JavaTypeParameterImpl) classifier);
            }
            else if (classifier instanceof JavaClassImpl) {
                JavaClassImpl javaClass = (JavaClassImpl) classifier;
                Map<JavaTypeParameterImpl, JavaTypeImpl<?>> substMap = new HashMap<JavaTypeParameterImpl, JavaTypeImpl<?>>();
                processClass(javaClass, classifierType.getSubstitutor(), substMap);

                return javaClass.createImmediateType(substMap);
            }

            return type;
        }
        else if (type instanceof JavaPrimitiveType) {
            return type;
        }
        else if (type instanceof JavaArrayTypeImpl) {
            JavaTypeImpl<?> componentType = ((JavaArrayTypeImpl) type).getComponentType();
            JavaTypeImpl<?> substitutedComponentType = substitute(componentType);
            if (substitutedComponentType == componentType) return type;

            return new JavaArrayTypeImpl(substitutedComponentType.getPsi().createArrayType());
        }
        else if (type instanceof JavaWildcardTypeImpl) {
            return substituteWildcardType((JavaWildcardTypeImpl) type);
        }

        return type;
    }

    private void processClass(
            @NotNull JavaClassImpl javaClass,
            @NotNull JavaTypeSubstitutorImpl substitutor,
            @NotNull Map<JavaTypeParameterImpl, JavaTypeImpl<?>> substMap
    ) {
        @SuppressWarnings("unchecked")
        List<JavaTypeParameterImpl> typeParameters = (List) javaClass.getTypeParameters();
        for (JavaTypeParameterImpl typeParameter : typeParameters) {
            JavaTypeImpl<?> substitutedParam = substitutor.substitute(typeParameter);
            substMap.put(typeParameter, substitutedParam == null ? null : substituteInternal(substitutedParam));
        }

        if (javaClass.isStatic()) {
            return;
        }

        JavaClassImpl outerClass = javaClass.getOuterClass();
        if (outerClass != null) {
            processClass(outerClass, substitutor, substMap);
        }
    }

    @Nullable
    private JavaTypeImpl<?> substituteWildcardType(@NotNull JavaWildcardTypeImpl wildcardType) {
        JavaTypeImpl<?> bound = wildcardType.getBound();
        if (bound == null) {
            return wildcardType;
        }

        JavaTypeImpl<?> newBound = substituteInternal(bound);
        if (newBound == null) {
            // This can be in case of substitution wildcard to raw type
            return null;
        }

        return rebound(wildcardType, newBound);
    }

    @NotNull
    private static JavaWildcardTypeImpl rebound(@NotNull JavaWildcardTypeImpl type, @NotNull JavaTypeImpl<?> newBound) {
        PsiManager manager = type.getPsi().getManager();
        if (createJavaLangObjectType(manager).equals(newBound)) {
            return createUnboundedWildcard(manager);
        }

        if (type.isExtends()) {
            return createUpperBoundWildcard(manager, newBound);
        }
        else {
            return createLowerBoundWildcard(manager, newBound);
        }
    }

    @NotNull
    private JavaTypeImpl<?> rawTypeForTypeParameter(@NotNull JavaTypeParameterImpl typeParameter) {
        Collection<JavaClassifierType> bounds = typeParameter.getUpperBounds();
        if (!bounds.isEmpty()) {
            return substitute(((JavaClassifierTypeImpl) bounds.iterator().next()));
        }

        return createJavaLangObjectType(typeParameter.getPsi().getManager());
    }

    @Nullable
    public JavaTypeImpl<?> substitute(@NotNull JavaTypeParameterImpl typeParameter) {
        if (substitutionMap.containsKey(typeParameter)) {
            return substitutionMap.get(typeParameter);
        }

        PsiTypeParameter psiTypeParameter = typeParameter.getPsi();
        return JavaTypeImpl.create(
                JavaPsiFacade.getInstance(psiTypeParameter.getProject()).getElementFactory().createType(psiTypeParameter)
        );
    }

    @Override
    public int hashCode() {
        return substitutionMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaTypeSubstitutorImpl && substitutionMap.equals(((JavaTypeSubstitutorImpl) obj).substitutionMap);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + substitutionMap;
    }

    @NotNull
    private static JavaTypeImpl<?> createJavaLangObjectType(@NotNull PsiManager manager) {
        return JavaTypeImpl.create(PsiType.getJavaLangObject(manager, GlobalSearchScope.allScope(manager.getProject())));
    }

    @NotNull
    private static JavaWildcardTypeImpl createUpperBoundWildcard(@NotNull PsiManager manager, @NotNull JavaTypeImpl<?> bound) {
        return new JavaWildcardTypeImpl(PsiWildcardType.createExtends(manager, bound.getPsi()));
    }

    @NotNull
    private static JavaWildcardTypeImpl createLowerBoundWildcard(@NotNull PsiManager manager, @NotNull JavaTypeImpl<?> bound) {
        return new JavaWildcardTypeImpl(PsiWildcardType.createSuper(manager, bound.getPsi()));
    }

    @NotNull
    private static JavaWildcardTypeImpl createUnboundedWildcard(@NotNull PsiManager manager) {
        return new JavaWildcardTypeImpl(PsiWildcardType.createUnbounded(manager));
    }
}
