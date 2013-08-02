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

package org.jetbrains.jet.plugin.findUsages.handlers;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory;

import java.util.Collection;
import java.util.Collections;

public abstract class KotlinFindUsagesHandler<T extends PsiElement> extends FindUsagesHandler {
    private final KotlinFindUsagesHandlerFactory factory;
    private final Collection<? extends PsiElement> elementsToSearch;

    public KotlinFindUsagesHandler(
            @NotNull T psiElement,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory
    ) {
        super(psiElement);
        this.factory = factory;
        this.elementsToSearch = elementsToSearch;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T getElement() {
        return (T)getPsiElement();
    }

    public KotlinFindUsagesHandler(@NotNull T psiElement, @NotNull KotlinFindUsagesHandlerFactory factory) {
        this(psiElement, Collections.<PsiElement>emptyList(), factory);
    }

    public final KotlinFindUsagesHandlerFactory getFactory() {
        return factory;
    }

    @NotNull
    @Override
    public PsiElement[] getPrimaryElements() {
        return elementsToSearch.isEmpty()
               ? new PsiElement[] {getPsiElement()}
               : elementsToSearch.toArray(new PsiElement[elementsToSearch.size()]);
    }
}
