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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.typeParameters;
import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.valueParameters;

public class JavaMethod extends JavaMemberImpl implements JavaTypeParameterListOwner {
    public JavaMethod(@NotNull PsiMethod psiMethod) {
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

    // TODO: NotNull?
    @Nullable
    @Override
    public JavaClass getContainingClass() {
        PsiClass psiClass = getPsi().getContainingClass();
        return psiClass == null ? null : new JavaClass(psiClass);
    }

    @NotNull
    @Override
    public Collection<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters());
    }

    @NotNull
    public Collection<JavaValueParameter> getValueParameters() {
        return valueParameters(getPsi().getParameterList().getParameters());
    }
}
