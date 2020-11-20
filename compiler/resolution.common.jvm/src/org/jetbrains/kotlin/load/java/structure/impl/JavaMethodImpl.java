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

import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.Name;

import java.util.List;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.typeParameters;
import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.valueParameters;

public class JavaMethodImpl extends JavaMemberImpl<PsiMethod> implements JavaMethod {
    public JavaMethodImpl(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
        assert !psiMethod.isConstructor() :
                "PsiMethod which is a constructor should be wrapped in JavaConstructorImpl: " + psiMethod.getName();
    }

    @NotNull
    @Override
    public Name getName() {
        return Name.identifier(getPsi().getName());
    }

    @NotNull
    @Override
    public List<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters());
    }

    @Override
    @NotNull
    public List<JavaValueParameter> getValueParameters() {
        return valueParameters(getPsi().getParameterList().getParameters());
    }

    @Override
    @Nullable
    public JavaAnnotationArgument getAnnotationParameterDefaultValue() {
        PsiMethod psiMethod = getPsi();
        if (psiMethod instanceof PsiAnnotationMethod) {
            PsiAnnotationMemberValue defaultValue = ((PsiAnnotationMethod) psiMethod).getDefaultValue();
            if (defaultValue != null) {
                return JavaAnnotationArgumentImpl.Factory.create(defaultValue, null);
            }
        }

        return null;
    }

    @Override
    public boolean getHasAnnotationParameterDefaultValue() {
        PsiMethod psiMethod = getPsi();
        return psiMethod instanceof PsiAnnotationMethod && ((PsiAnnotationMethod) psiMethod).getDefaultValue() != null;
    }

    @Override
    @NotNull
    public JavaType getReturnType() {
        PsiType psiType = getPsi().getReturnType();
        assert psiType != null : "Method is not a constructor and has no return type: " + getName();
        return JavaTypeImpl.create(psiType);
    }
}
