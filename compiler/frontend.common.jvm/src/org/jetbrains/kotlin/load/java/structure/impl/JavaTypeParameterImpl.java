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
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.classifierTypes;

public class JavaTypeParameterImpl extends JavaClassifierImpl<PsiTypeParameter> implements JavaTypeParameter {
    public JavaTypeParameterImpl(@NotNull JavaElementPsiSource<PsiTypeParameter> psiTypeParameterSource) {
        super(psiTypeParameterSource);
    }

    @SuppressWarnings("unused") // used in KSP
    public JavaTypeParameterImpl(PsiTypeParameter psiTypeParameter) {
        this(JavaElementSourceFactory.getInstance(psiTypeParameter.getProject()).createPsiSource(psiTypeParameter));
    }

    @NotNull
    @Override
    public Name getName() {
        return SpecialNames.safeIdentifier(getPsi().getName());
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getUpperBounds() {
        PsiClassType[] referencedTypes = getPsi().getExtendsList().getReferencedTypes();
        if (referencedTypes.length == 0) return Collections.emptyList();
        Collection<JavaClassifierType> result = new ArrayList<>(referencedTypes.length);
        JavaElementSourceFactory sourceFactory = getSourceFactory();
        for (int boundIndex = 0; boundIndex < referencedTypes.length; boundIndex++) {
            JavaElementTypeSource<PsiClassType> source =
                    sourceFactory.createTypeParameterUpperBoundTypeSource(psiElementSource, boundIndex);
            result.add(new JavaClassifierTypeImpl(source));
        }
        return result;
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi();
    }
}
