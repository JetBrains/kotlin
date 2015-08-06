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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaArrayType;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;

public abstract class JavaTypeImpl<Psi extends PsiType> implements JavaType, JavaAnnotationOwnerImpl {
    private final Psi psiType;

    public JavaTypeImpl(@NotNull Psi psiType) {
        this.psiType = psiType;
    }

    @NotNull
    public Psi getPsi() {
        return psiType;
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi();
    }

    @NotNull
    public static JavaTypeImpl<?> create(@NotNull PsiType psiType) {
        return psiType.accept(new PsiTypeVisitor<JavaTypeImpl<?>>() {
            @Nullable
            @Override
            public JavaTypeImpl<?> visitType(@NotNull PsiType type) {
                throw new UnsupportedOperationException("Unsupported PsiType: " + type);
            }

            @Nullable
            @Override
            public JavaTypeImpl<?> visitPrimitiveType(@NotNull PsiPrimitiveType primitiveType) {
                return new JavaPrimitiveTypeImpl(primitiveType);
            }

            @Nullable
            @Override
            public JavaTypeImpl<?> visitArrayType(@NotNull PsiArrayType arrayType) {
                return new JavaArrayTypeImpl(arrayType);
            }

            @Nullable
            @Override
            public JavaTypeImpl<?> visitClassType(@NotNull PsiClassType classType) {
                return new JavaClassifierTypeImpl(classType);
            }

            @Nullable
            @Override
            public JavaTypeImpl<?> visitWildcardType(@NotNull PsiWildcardType wildcardType) {
                return new JavaWildcardTypeImpl(wildcardType);
            }
        });
    }

    @NotNull
    @Override
    public JavaArrayType createArrayType() {
        return new JavaArrayTypeImpl(getPsi().createArrayType());
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        return JavaElementUtil.getAnnotations(this);
    }

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName);
    }

    @Override
    public boolean isDeprecatedInJavaDoc() {
        return false;
    }

    @Override
    public int hashCode() {
        return getPsi().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaTypeImpl && getPsi().equals(((JavaTypeImpl) obj).getPsi());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getPsi();
    }
}
