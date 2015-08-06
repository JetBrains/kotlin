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

import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaClassifier;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;

public abstract class JavaClassifierImpl<Psi extends PsiClass> extends JavaElementImpl<Psi> implements JavaClassifier, JavaAnnotationOwnerImpl {
    protected JavaClassifierImpl(@NotNull Psi psiClass) {
        super(psiClass);
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi().getModifierList();
    }

    @NotNull
    /* package */ static JavaClassifier create(@NotNull PsiClass psiClass) {
        if (psiClass instanceof PsiTypeParameter) {
            return new JavaTypeParameterImpl((PsiTypeParameter) psiClass);
        }
        else {
            return new JavaClassImpl(psiClass);
        }
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
        return getPsi().isDeprecated();
    }
}
