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
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;

import java.util.Collection;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.classifierTypes;

public class JavaTypeParameterImpl extends JavaClassifierImpl<PsiTypeParameter> implements JavaTypeParameter {
    public JavaTypeParameterImpl(@NotNull JavaElementPsiSource<PsiTypeParameter> psiTypeParameterSource) {
        super(psiTypeParameterSource);
    }

    @NotNull
    @Override
    public Name getName() {
        return SpecialNames.safeIdentifier(getPsi().getName());
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getUpperBounds() {
        return classifierTypes(getPsi().getExtendsList().getReferencedTypes(), getSourceFactory());
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi();
    }
}
