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

package org.jetbrains.jet.plugin.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;

public abstract class AbstractPolyVariantJetReference<T extends JetElement> implements PsiPolyVariantReference {
    protected final T element;

    public AbstractPolyVariantJetReference(@NotNull T element) {
        this.element = element;
    }

    @Override
    public T getElement() {
        return element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] results = multiResolve(false);
        if (results.length == 1) return results[0].getElement();
        return null;
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        if (element == null) return false;

        ResolveResult[] results = multiResolve(false);
        for (ResolveResult result : results) {
            if (element.equals(result.getElement())) return true;
        }
        return false;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean isSoft() {
        return false;
    }
}
