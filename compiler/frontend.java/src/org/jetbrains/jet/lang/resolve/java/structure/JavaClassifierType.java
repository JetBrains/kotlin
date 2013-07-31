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
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaClassifierType extends JavaType {
    public JavaClassifierType(@NotNull PsiClassType psiClassType) {
        super(psiClassType);
    }

    @NotNull
    @Override
    public PsiClassType getPsi() {
        return (PsiClassType) super.getPsi();
    }

    @Nullable
    public JavaClassifier resolve() {
        PsiClass psiClass = getPsi().resolve();
        return psiClass == null ? null : JavaClassifier.create(psiClass);
    }

    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        PsiType[] psiTypes = getPsi().getSuperTypes();
        if (psiTypes.length == 0) return Collections.emptyList();
        List<JavaClassifierType> result = new ArrayList<JavaClassifierType>(psiTypes.length);
        for (PsiType psiType : psiTypes) {
            if (!(psiType instanceof PsiClassType)) {
                throw new IllegalStateException("Supertype should be a class: " + psiType + ", type: " + getPsi());
            }
            result.add(new JavaClassifierType((PsiClassType) psiType));
        }
        return result;
    }

    @NotNull
    public String getPresentableText() {
        return getPsi().getPresentableText();
    }
}
