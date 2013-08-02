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

package org.jetbrains.jet.lang.resolve.java.structure;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class JavaType {
    private final PsiType psiType;

    public JavaType(@NotNull PsiType psiType) {
        this.psiType = psiType;
    }

    @NotNull
    public PsiType getPsi() {
        return psiType;
    }

    @NotNull
    public static JavaType create(@NotNull PsiType psiType) {
        return psiType.accept(new PsiTypeVisitor<JavaType>() {
            @Nullable
            @Override
            public JavaType visitType(PsiType type) {
                throw new UnsupportedOperationException("Unsupported PsiType: " + type);
            }

            @Nullable
            @Override
            public JavaType visitPrimitiveType(PsiPrimitiveType primitiveType) {
                return new JavaPrimitiveType(primitiveType);
            }

            @Nullable
            @Override
            public JavaType visitArrayType(PsiArrayType arrayType) {
                return new JavaArrayType(arrayType);
            }

            @Nullable
            @Override
            public JavaType visitClassType(PsiClassType classType) {
                return new JavaClassifierType(classType);
            }

            @Nullable
            @Override
            public JavaType visitWildcardType(PsiWildcardType wildcardType) {
                return new JavaWildcardType(wildcardType);
            }
        });
    }

    @Override
    public int hashCode() {
        return getPsi().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaType && getPsi().equals(((JavaType) obj).getPsi());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getPsi();
    }
}
