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

import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.typeParameters;
import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.valueParameters;

public class JavaMethodImpl extends JavaMemberImpl implements JavaMethod {
    public JavaMethodImpl(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
    }

    @NotNull
    @Override
    public PsiMethod getPsi() {
        return (PsiMethod) super.getPsi();
    }

    @NotNull
    @Override
    public Name getName() {
        return Name.identifier(getPsi().getName());
    }

    @NotNull
    @Override
    public Collection<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters());
    }

    @Override
    @NotNull
    public Collection<JavaValueParameter> getValueParameters() {
        return valueParameters(getPsi().getParameterList().getParameters());
    }

    @Override
    public boolean hasAnnotationParameterDefaultValue() {
        PsiMethod psiMethod = getPsi();
        return psiMethod instanceof PsiAnnotationMethod && ((PsiAnnotationMethod) psiMethod).getDefaultValue() != null;
    }

    @Override
    @Nullable
    public JavaType getReturnType() {
        PsiType psiType = getPsi().getReturnType();
        return psiType == null ? null : JavaType.create(psiType);
    }

    @Override
    public boolean isVararg() {
        return getPsi().isVarArgs();
    }

    @Override
    public boolean isConstructor() {
        // TODO: class JavaConstructor extends JavaMethod
        return getPsi().isConstructor();
    }
}
