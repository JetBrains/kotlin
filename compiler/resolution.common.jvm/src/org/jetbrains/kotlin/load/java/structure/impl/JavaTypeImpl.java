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
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaFixedElementSourceFactory;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaSourceFactoryOwner;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;
import java.util.function.Function;

public abstract class JavaTypeImpl<Psi extends PsiType> implements JavaType, JavaAnnotationOwnerImpl, JavaSourceFactoryOwner {
    private final JavaElementTypeSource<Psi> psiType;

    public JavaTypeImpl(@NotNull JavaElementTypeSource<Psi> psiTypeSource) {
        this.psiType = psiTypeSource;
    }

    @Override
    @NotNull
    public JavaElementSourceFactory getSourceFactory() {
        return psiType.getFactory();
    }


    @NotNull
    public Psi getPsi() {
        return psiType.getType();
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi();
    }

    @NotNull
    public static JavaTypeImpl<?> create(JavaElementTypeSource<? extends PsiType> psiTypeSource) {
        return create(psiTypeSource.getType(), psiTypeSource);
    }

    /**
     * @deprecated used only for a source/binary compatibility with existing 3rd party tools. Should not be used from Analysis API, Kotlin compiler, or from any other place in the Kotlin repository.
     */
    @NotNull
    @Deprecated
    public static JavaTypeImpl<?> create(PsiType psiType) {
        // The JavaFixedElementSourceFactory is directly created here.
        // Instead, the `JavaElementSourceFactory.getInstance(project)` should be called, but in order to do this we need a `com.intellij.openapi.project.Project` instance
        return create(new JavaFixedElementSourceFactory().createTypeSource(psiType));
    }


    @NotNull
    public static JavaTypeImpl<?> create(@NotNull PsiType psiType, JavaElementTypeSource<? extends PsiType> psiTypeSource) {
        return psiType.accept(new PsiTypeVisitor<JavaTypeImpl<?>>() {

            @Nullable
            @Override
            public JavaTypeImpl<?> visitType(@NotNull PsiType type) {
                throw new UnsupportedOperationException("Unsupported PsiType: " + type);
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public JavaTypeImpl<?> visitPrimitiveType(@NotNull PsiPrimitiveType primitiveType) {
                return new JavaPrimitiveTypeImpl((JavaElementTypeSource<PsiPrimitiveType>)psiTypeSource );
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public JavaTypeImpl<?> visitArrayType(@NotNull PsiArrayType arrayType) {
                return new JavaArrayTypeImpl((JavaElementTypeSource<PsiArrayType>) psiTypeSource);
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public JavaTypeImpl<?> visitClassType(@NotNull PsiClassType classType) {
                return new JavaClassifierTypeImpl((JavaElementTypeSource<PsiClassType>) psiTypeSource);
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public JavaTypeImpl<?> visitWildcardType(@NotNull PsiWildcardType wildcardType) {
                return new JavaWildcardTypeImpl((JavaElementTypeSource<PsiWildcardType>) psiTypeSource);
            }
        });
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        return JavaElementUtil.getAnnotations(this, getSourceFactory());
    }

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName, getSourceFactory());
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
