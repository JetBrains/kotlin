/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.impl.light.LightEmptyImplementsList;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetLanguage;

/**
 * This class contains method that are straightforward, and only would pollute the actual implementation
 */
public abstract class KotlinLightClassForPackageBase extends AbstractLightClass {

    private final PsiModifierList modifierList;
    private final LightEmptyImplementsList implementsList;

    public KotlinLightClassForPackageBase(PsiManager manager) {
        super(manager, JetLanguage.INSTANCE);
        this.modifierList = new LightModifierList(manager, JetLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL);
        this.implementsList = new LightEmptyImplementsList(manager);
    }

    @Nullable
    @Override
    public PsiModifierList getModifierList() {
        return modifierList;
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        return modifierList.hasModifierProperty(name);
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAnnotationType() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Nullable
    @Override
    public PsiClass getContainingClass() {
        return null;
    }

    @Override
    public boolean hasTypeParameters() {
        return false;
    }

    @NotNull
    @Override
    public PsiTypeParameter[] getTypeParameters() {
        return PsiTypeParameter.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public PsiDocComment getDocComment() {
        return null;
    }

    @Nullable
    @Override
    public PsiReferenceList getImplementsList() {
        return implementsList;
    }

    @NotNull
    @Override
    public PsiClassType[] getImplementsListTypes() {
        return PsiClassType.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiReferenceList getExtendsList() {
        // TODO: Find a way to return just Object
        return super.getExtendsList();
    }

    @NotNull
    @Override
    public PsiClassType[] getExtendsListTypes() {
        // TODO see getExtendsList()
        return super.getExtendsListTypes();
    }

    @Nullable
    @Override
    public PsiClass getSuperClass() {
        // TODO see getExtendsList()
        return super.getSuperClass();
    }

    @NotNull
    @Override
    public PsiClass[] getSupers() {
        // TODO see getExtendsList()
        return super.getSupers();
    }

    @NotNull
    @Override
    public PsiClassType[] getSuperTypes() {
        // TODO see getExtendsList()
        return super.getSuperTypes();
    }

    @Override
    public PsiClass[] getInterfaces() {
        return PsiClass.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public PsiClass[] getInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public PsiClassInitializer[] getInitializers() {
        return PsiClassInitializer.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public PsiClass[] getAllInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiClass findInnerClassByName(@NonNls String name, boolean checkBases) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getLBrace() {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getRBrace() {
        return null;
    }
}
