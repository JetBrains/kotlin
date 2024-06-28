/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.lang.Language;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

// Based on com.intellij.psi.impl.light.LightVariableBuilder
public class LightVariableBuilder extends LightElement implements PsiVariable, NavigationItem {
    private final String myName;
    private final PsiType myType;
    private final LightModifierList myModifierList;

    public LightVariableBuilder(PsiManager manager, @NotNull String name, @NotNull PsiType type, Language language) {
        super(manager, language);
        myName = name;
        myType = type;
        myModifierList = new LightModifierList(manager);
    }

    @Override
    public String toString() {
        return "LightVariableBuilder:" + getName();
    }

    @NotNull
    @Override
    public PsiType getType() {
        return myType;
    }

    @Override
    @NotNull
    public PsiModifierList getModifierList() {
        return myModifierList;
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        return myModifierList.hasModifierProperty(name);
    }

    @NotNull
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PsiTypeElement getTypeElement() {
        return null;
    }

    @Override
    public PsiExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException {
    }

    @Override
    public Object computeConstantValue() {
        return null;
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return null;
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException("setName is not implemented yet in org.jetbrains.kotlin.asJava.light.LightVariableBuilder");
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }
}
