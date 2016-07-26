/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.lang.Language;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.*;
import com.intellij.psi.impl.ElementPresentationUtil;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.ui.RowIcon;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

    @Override
    public Icon getElementIcon(int flags) {
        RowIcon baseIcon = ElementPresentationUtil.createLayeredIcon(PlatformIcons.VARIABLE_ICON, this, false);
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
    }
}
