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

public final class JetStubbedPsiUtil {
    @Nullable
    public static JetDeclaration getContainingDeclaration(@NotNull PsiElement element) {
        return getPsiOrStubParent(element, JetDeclaration.class, true);
    }

    @Nullable
    public static <T extends JetDeclaration> T getContainingDeclaration(@NotNull PsiElement element, @NotNull Class<T> declarationClass) {
        return getPsiOrStubParent(element, declarationClass, true);
    }

    //TODO: contribute to idea PsiTreeUtil#getPsiOrStubParent
    @Nullable
    public static <T extends JetElement> T getPsiOrStubParent(
            @NotNull PsiElement element,
            @NotNull Class<T> declarationClass,
            boolean strict
    ) {
        if (!strict && declarationClass.isInstance(element)) {
            //noinspection unchecked
            return (T) element;
        }
        if (element instanceof JetElementImplStub) {
            StubElement<?> stub = ((JetElementImplStub) element).getStub();
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass);
            }
        }
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict);
    }

    @Nullable
    public static <T extends JetElement> T getStubOrPsiChild(
            @NotNull JetElementImplStub<?> element,
            @NotNull TokenSet types,
            @NotNull ArrayFactory<T> factory
    ) {
        T[] typeElements = element.getStubOrPsiChildren(types, factory);
        if (typeElements.length == 0) {
            return null;
        }
        return typeElements[0];
    }

    private JetStubbedPsiUtil() {
    }
}
