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

package org.jetbrains.kotlin.asJava;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightModifierList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public abstract class KtLightModifierList extends LightModifierList {
    public KtLightModifierList(PsiManager psiManager, @NotNull String[] modifiers) {
        super(psiManager, KotlinLanguage.INSTANCE, modifiers);
    }

    public abstract PsiAnnotationOwner getDelegate();

    @Override
    @NotNull
    public PsiAnnotation[] getAnnotations() {
        return getDelegate().getAnnotations();
    }

    @Override
    @NotNull
    public PsiAnnotation[] getApplicableAnnotations() {
        return getDelegate().getApplicableAnnotations();
    }

    @Override
    @Nullable
    public PsiAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        return getDelegate().findAnnotation(qualifiedName);
    }

    @Override
    @NotNull
    public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        return getDelegate().addAnnotation(qualifiedName);
    }
}
