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

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static helpers for navigating the Kotlin PSI in a way that also works over stubs.
 *
 * <p>The platform's {@link PsiTreeUtil} operates on the AST tree, which forces a stub-backed element to be parsed.
 * The methods here (such as {@link #getContainingDeclaration} and {@link #getPsiOrStubParent}) instead walk the stub
 * hierarchy when one is available, avoiding unnecessary parsing.
 */
public final class KtStubbedPsiUtil {
    /**
     * Returns the nearest enclosing {@link KtDeclaration} of the given element (excluding the element itself), or
     * {@code null} if there is none.
     */
    @Nullable
    public static KtDeclaration getContainingDeclaration(@NotNull PsiElement element) {
        return getPsiOrStubParent(element, KtDeclaration.class, true);
    }

    /**
     * Returns the nearest enclosing {@link KtDeclaration} of the given element that is an instance of
     * {@code declarationClass} (excluding the element itself), or {@code null} if there is none.
     */
    @Nullable
    public static <T extends KtDeclaration> T getContainingDeclaration(@NotNull PsiElement element, @NotNull Class<T> declarationClass) {
        return getPsiOrStubParent(element, declarationClass, true);
    }

    //TODO: contribute to idea PsiTreeUtil#getPsiOrStubParent
    /**
     * Returns the nearest parent of the given element assignable to {@code declarationClass}, or {@code null} if there
     * is none. When {@code strict} is {@code false}, the element itself is also considered a candidate.
     *
     * <p>When the element is backed by a stub, its stub hierarchy is walked instead of the AST, avoiding parsing;
     * otherwise this falls back to {@link PsiTreeUtil#getParentOfType}.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends KtElement> T getPsiOrStubParent(
            @NotNull PsiElement element,
            @NotNull Class<T> declarationClass,
            boolean strict
    ) {
        if (!strict && declarationClass.isInstance(element)) {
            return (T) element;
        }
        if (element instanceof KtElementImplStub) {
            StubElement<?> stub = ((KtElementImplStub) element).getStub();
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass);
            }
        }
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict);
    }

    /**
     * Returns the first child of the given element whose type is in {@code types}, or {@code null} if there is none.
     * Stub children are used when the element is backed by a stub, avoiding parsing; otherwise its PSI children are
     * traversed. The {@code factory} produces the typed array used to collect the matching children.
     */
    @Nullable
    public static <T extends KtElement> T getStubOrPsiChild(
            @NotNull KtElementImplStub<?> element,
            @NotNull TokenSet types,
            @NotNull ArrayFactory<T> factory
    ) {
        T[] typeElements = element.getStubOrPsiChildren(types, factory);
        if (typeElements.length == 0) {
            return null;
        }
        return typeElements[0];
    }

    private KtStubbedPsiUtil() {
    }
}
