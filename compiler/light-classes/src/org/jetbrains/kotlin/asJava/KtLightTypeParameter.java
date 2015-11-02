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

import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner;

public class KtLightTypeParameter
        extends AbstractLightClass implements PsiTypeParameter, KtLightElement<KtTypeParameter, PsiTypeParameter> {
    private final PsiTypeParameterListOwner owner;
    private final int index;
    private final String name;

    protected KtLightTypeParameter(
            @NotNull PsiTypeParameterListOwner owner,
            int index,
            @NotNull String name) {
        super(owner.getManager(), KotlinLanguage.INSTANCE);
        this.owner = owner;
        this.index = index;
        this.name = name;
    }

    @NotNull
    @Override
    public PsiTypeParameter getDelegate() {
        return getOwnerDelegate().getTypeParameters()[index];
    }

    @NotNull
    @Override
    public KtTypeParameter getOrigin() {
        KtTypeParameterListOwner jetOwner = (KtTypeParameterListOwner) LightClassUtilsKt.getUnwrapped(owner);
        assert (jetOwner != null) : "Invalid type parameter owner: " + owner;

        return jetOwner.getTypeParameters().get(index);
    }

    @NotNull
    private PsiTypeParameterListOwner getOwnerDelegate() {
        if (owner instanceof KtLightClass) return ((KtLightClass) owner).getDelegate();
        if (owner instanceof KtLightMethod) return ((KtLightMethod) owner).getDelegate();
        return owner;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KtLightTypeParameter(owner, index, name);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor) visitor).visitTypeParameter(this);
        }
        else {
            super.accept(visitor);
        }
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Override
    public PsiTypeParameterListOwner getOwner() {
        return owner;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    @Override
    public PsiAnnotation[] getAnnotations() {
        return getDelegate().getAnnotations();
    }

    @NotNull
    @Override
    public PsiAnnotation[] getApplicableAnnotations() {
        return getDelegate().getApplicableAnnotations();
    }

    @Override
    public PsiAnnotation findAnnotation(@NotNull String qualifiedName) {
        return getDelegate().findAnnotation(qualifiedName);
    }

    @NotNull
    @Override
    public PsiAnnotation addAnnotation(@NotNull String qualifiedName) {
        return getDelegate().addAnnotation(qualifiedName);
    }

    @Override
    public String toString() {
        return "KotlinLightTypeParameter:" + getName();
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return getOrigin();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return getOrigin().getUseScope();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof KtLightTypeParameter && getOrigin().equals(((KtLightTypeParameter) obj).getOrigin());
    }
}
