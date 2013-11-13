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

package org.jetbrains.jet.asJava.light;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

// Based on com.intellij.psi.impl.light.LightVariableBuilder
public class LightVariableBuilder<T extends LightVariableBuilder> extends LightElement implements PsiVariable, NavigationItem {
    private final String myName;
    private final PsiType myType;
    private volatile LightModifierList myModifierList;
    private volatile Icon myBaseIcon = PlatformIcons.VARIABLE_ICON;
    private String myOriginInfo;

    public LightVariableBuilder(@NotNull String name, @NotNull String type, @NotNull PsiElement navigationElement) {
        this(name, JavaPsiFacade.getElementFactory(navigationElement.getProject()).createTypeFromText(type, navigationElement), navigationElement);
    }

    public LightVariableBuilder(@NotNull String name, @NotNull PsiType type, @NotNull PsiElement navigationElement) {
        this(navigationElement.getManager(), name, type, JavaLanguage.INSTANCE);
        setNavigationElement(navigationElement);
    }

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

    public T setModifiers(String... modifiers) {
        myModifierList = new LightModifierList(getManager(), getLanguage(), modifiers);
        return (T)this;
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
        throw new UnsupportedOperationException("setName is not implemented yet in org.jetbrains.jet.asJava.light.LightVariableBuilder");
    }

    @Override
    protected boolean isVisibilitySupported() {
        return true;
    }

    @Override
    public Icon getElementIcon(final int flags) {
        final RowIcon baseIcon = ElementPresentationUtil.createLayeredIcon(myBaseIcon, this, false);
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon);
    }

    public T setBaseIcon(Icon baseIcon) {
        myBaseIcon = baseIcon;
        return (T)this;
    }

    //@Nullable
    //@Override
    //public String getOriginInfo() {
    //    return myOriginInfo;
    //}

    public void setOriginInfo(@Nullable String originInfo) {
        myOriginInfo = originInfo;
    }
}
