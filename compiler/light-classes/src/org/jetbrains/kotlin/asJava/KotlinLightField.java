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
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.psi.JetDeclaration;

// Copied from com.intellij.psi.impl.light.LightField
public abstract class KotlinLightField<T extends JetDeclaration, D extends PsiField> extends LightElement
        implements PsiField, KotlinLightElement<T, D> {
    private final T origin;
    private final D delegate;
    private final PsiClass containingClass;

    public KotlinLightField(@NotNull PsiManager manager, @NotNull T origin, @NotNull D delegate, @NotNull PsiClass containingClass) {
        super(manager, JavaLanguage.INSTANCE);
        this.origin = origin;
        this.delegate = delegate;
        this.containingClass = containingClass;
    }

    @NotNull
    @Override
    public abstract KotlinLightField<T, D> copy();

    @Override
    public void setInitializer(@Nullable PsiExpression initializer) throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return origin.getUseScope();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @NotNull
    @Override
    public PsiIdentifier getNameIdentifier() {
        return delegate.getNameIdentifier();
    }

    @Override
    public PsiDocComment getDocComment() {
        return delegate.getDocComment();
    }

    @Override
    public boolean isDeprecated() {
        return delegate.isDeprecated();
    }

    @Override
    public PsiClass getContainingClass() {
        return containingClass;
    }

    @NotNull
    @Override
    public PsiType getType() {
        return delegate.getType();
    }

    @Override
    public PsiTypeElement getTypeElement() {
        return delegate.getTypeElement();
    }

    @Override
    public PsiExpression getInitializer() {
        return delegate.getInitializer();
    }

    @Override
    public boolean hasInitializer() {
        return delegate.hasInitializer();
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @Override
    public Object computeConstantValue() {
        return delegate.computeConstantValue();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Not supported");
    }

    @Override
    public PsiModifierList getModifierList() {
        return delegate.getModifierList();
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        return delegate.hasModifierProperty(name);
    }

    @Override
    public String getText() {
        return delegate.getText();
    }

    @Override
    public TextRange getTextRange() {
        return new TextRange(-1, -1);
    }

    @Override
    public boolean isValid() {
        return containingClass.isValid();
    }

    @Override
    public String toString() {
        return "PsiField:" + getName();
    }

    @NotNull
    @Override
    public T getOrigin() {
        return origin;
    }

    @NotNull
    @Override
    public D getDelegate() {
        return delegate;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return getOrigin();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        if (another instanceof KotlinLightField && origin.isEquivalentTo(((KotlinLightField) another).getOrigin())) {
            return true;
        }
        return super.isEquivalentTo(another);
    }
}
