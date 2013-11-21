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

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Copy of com.intellij.psi.impl.light.LightField
@SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter")
public class LightField extends LightElement implements PsiField {
    private final PsiField myField;
    private final PsiClass myContainingClass;

    public LightField(@NotNull final PsiManager manager, @NotNull final PsiField field, @NotNull final PsiClass containingClass) {
        super(manager, JavaLanguage.INSTANCE);
        myField = field;
        myContainingClass = containingClass;
    }

    @Override
    public void setInitializer(@Nullable final PsiExpression initializer) throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return myField.getUseScope();
    }

    @Override
    public String getName() {
        return myField.getName();
    }

    @NotNull
    @Override
    public PsiIdentifier getNameIdentifier() {
        return myField.getNameIdentifier();
    }

    @Override
    public PsiDocComment getDocComment() {
        return myField.getDocComment();
    }

    @Override
    public boolean isDeprecated() {
        return myField.isDeprecated();
    }

    @Override
    public PsiClass getContainingClass() {
        return myContainingClass;
    }

    @NotNull
    @Override
    public PsiType getType() {
        return myField.getType();
    }

    @Override
    public PsiTypeElement getTypeElement() {
        return myField.getTypeElement();
    }

    @Override
    public PsiExpression getInitializer() {
        return myField.getInitializer();
    }

    @Override
    public boolean hasInitializer() {
        return myField.hasInitializer();
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @Override
    public Object computeConstantValue() {
        return myField.computeConstantValue();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull final String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @Override
    public PsiModifierList getModifierList() {
        return myField.getModifierList();
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull final String name) {
        return myField.hasModifierProperty(name);
    }

    @Override
    public String getText() {
        return myField.getText();
    }

    @Override
    public PsiElement copy() {
        return new LightField(myManager, (PsiField)myField.copy(), myContainingClass);
    }

    @Override
    public TextRange getTextRange() {
        return new TextRange(-1, -1);
    }

    @Override
    public boolean isValid() {
        return myContainingClass.isValid();
    }

    @Override
    public String toString() {
        return "PsiField:" + getName();
    }
}
