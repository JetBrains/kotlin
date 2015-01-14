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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaTypeSubstitutorImpl implements JavaTypeSubstitutor {
    private final Map<JavaTypeParameter, JavaType> substitutionMap;

    public JavaTypeSubstitutorImpl(@NotNull Map<JavaTypeParameter, JavaType> substitutionMap) {
        this.substitutionMap = substitutionMap;
    }

    @NotNull
    @Override
    public JavaType substitute(@NotNull JavaType type) {
        JavaType substitutedType = substituteInternal(type);
        return substitutedType != null ? substitutedType : correctSubstitutionForRawType(type);
    }

    @NotNull
    // In case of raw type we get substitution map like T -> null,
    // in this case we should substitute upper bound of T or,
    // if it does not exist, return java.lang.Object
    private JavaType correctSubstitutionForRawType(@NotNull JavaType original) {
        if (original instanceof JavaClassifierType) {
            JavaClassifier classifier = ((JavaClassifierType) original).getClassifier();
            if (classifier instanceof JavaTypeParameter) {
                return rawTypeForTypeParameter((JavaTypeParameter) classifier);
            }
        }

        return original;
    }

    @Nullable
    private JavaType substituteInternal(@NotNull JavaType type) {
        if (type instanceof JavaClassifierType) {
            JavaClassifierType classifierType = (JavaClassifierType) type;
            JavaClassifier classifier = classifierType.getClassifier();

            if (classifier instanceof JavaTypeParameter) {
                return substitute((JavaTypeParameter) classifier);
            }
            else if (classifier instanceof JavaClass) {
                JavaClass javaClass = (JavaClass) classifier;
                Map<JavaTypeParameter, JavaType> substMap = new HashMap<JavaTypeParameter, JavaType>();
                processClass(javaClass, classifierType.getSubstitutor(), substMap);

                return javaClass.createImmediateType(new JavaTypeSubstitutorImpl(substMap));
            }

            return type;
        }
        else if (type instanceof JavaPrimitiveType) {
            return type;
        }
        else if (type instanceof JavaArrayType) {
            JavaType componentType = ((JavaArrayType) type).getComponentType();
            JavaType substitutedComponentType = substitute(componentType);
            if (substitutedComponentType == componentType) return type;

            return substitutedComponentType.createArrayType();
        }
        else if (type instanceof JavaWildcardType) {
            return substituteWildcardType((JavaWildcardType) type);
        }

        return type;
    }

    private void processClass(@NotNull JavaClass javaClass, @NotNull JavaTypeSubstitutor substitutor, @NotNull Map<JavaTypeParameter, JavaType> substMap) {
        List<JavaTypeParameter> typeParameters = javaClass.getTypeParameters();
        for (JavaTypeParameter typeParameter : typeParameters) {
            JavaType substitutedParam = substitutor.substitute(typeParameter);
            if (substitutedParam == null) {
                substMap.put(typeParameter, null);
            }
            else {
                substMap.put(typeParameter, substituteInternal(substitutedParam));
            }
        }

        if (javaClass.isStatic()) {
            return;
        }

        JavaClass outerClass = javaClass.getOuterClass();
        if (outerClass != null) {
            processClass(outerClass, substitutor, substMap);
        }
    }

    @Nullable
    private JavaType substituteWildcardType(@NotNull JavaWildcardType wildcardType) {
        JavaType bound = wildcardType.getBound();
        if (bound == null) {
            return wildcardType;
        }

        JavaType newBound = substituteInternal(bound);
        if (newBound == null) {
            // This can be in case of substitution wildcard to raw type
            return null;
        }

        return rebound(wildcardType, newBound);
    }

    @NotNull
    private static JavaWildcardType rebound(@NotNull JavaWildcardType type, @NotNull JavaType newBound) {
        if (type.getTypeProvider().createJavaLangObjectType().equals(newBound)) {
            return type.getTypeProvider().createUnboundedWildcard();
        }

        if (type.isExtends()) {
            return type.getTypeProvider().createUpperBoundWildcard(newBound);
        }
        else {
            return type.getTypeProvider().createLowerBoundWildcard(newBound);
        }
    }

    @NotNull
    private JavaType rawTypeForTypeParameter(@NotNull JavaTypeParameter typeParameter) {
        Collection<JavaClassifierType> bounds = typeParameter.getUpperBounds();
        if (!bounds.isEmpty()) {
            return substitute(bounds.iterator().next());
        }

        return typeParameter.getTypeProvider().createJavaLangObjectType();
    }

    @Override
    @Nullable
    public JavaType substitute(@NotNull JavaTypeParameter typeParameter) {
        if (substitutionMap.containsKey(typeParameter)) {
            return substitutionMap.get(typeParameter);
        }

        return typeParameter.getType();
    }

    @Override
    @NotNull
    public Map<JavaTypeParameter, JavaType> getSubstitutionMap() {
        return substitutionMap;
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
}
