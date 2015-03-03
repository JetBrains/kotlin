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
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.classifierTypes;

public class JavaTypeParameterImpl extends JavaClassifierImpl<PsiTypeParameter> implements JavaTypeParameter {
    public JavaTypeParameterImpl(@NotNull PsiTypeParameter psiTypeParameter) {
        super(psiTypeParameter);
    }

    @NotNull
    @Override
    public Name getName() {
        return Name.identifier(getPsi().getName());
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getUpperBounds() {
        return classifierTypes(getPsi().getExtendsList().getReferencedTypes());
    }

    @Override
    @Nullable
    public JavaTypeParameterListOwner getOwner() {
        PsiTypeParameterListOwner owner = getPsi().getOwner();
        // TODO: a separate factory for such things
        if (owner instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) owner;
            return psiMethod.isConstructor() ? new JavaConstructorImpl(psiMethod) : new JavaMethodImpl(psiMethod);
        }
        else if (owner instanceof PsiClass) {
            return new JavaClassImpl((PsiClass) owner);
        }
        else if (owner != null) {
            throw new UnsupportedOperationException("Unsupported type parameter list owner: " + owner);
        }

        return null;
    }

    @NotNull
    @Override
    public JavaType getType() {
        return JavaTypeImpl.create(JavaPsiFacade.getInstance(getPsi().getProject()).getElementFactory().createType(getPsi()));
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return new JavaTypeProviderImpl(getPsi().getManager());
    }
}
