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

package org.jetbrains.jet.asJava;

import com.intellij.psi.*;
import com.intellij.psi.impl.light.AbstractLightClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetLanguage;

public class KotlinLightTypeParameter extends AbstractLightClass implements PsiTypeParameter {

    private final KotlinLightClassForExplicitDeclaration lightClass;
    private final int index;
    private final String name;

    protected KotlinLightTypeParameter(
            @NotNull KotlinLightClassForExplicitDeclaration lightClass,
            int index,
            @NotNull String name) {
        super(lightClass.getManager(), JetLanguage.INSTANCE);
        this.lightClass = lightClass;
        this.index = index;
        this.name = name;
    }

    @NotNull
    @Override
    public PsiTypeParameter getDelegate() {
        return lightClass.getDelegate().getTypeParameters()[index];
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightTypeParameter(lightClass, index, name);
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
        return lightClass;
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
}
