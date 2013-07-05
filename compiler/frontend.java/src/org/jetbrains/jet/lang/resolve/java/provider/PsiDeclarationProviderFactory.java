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

package org.jetbrains.jet.lang.resolve.java.provider;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;

public final class PsiDeclarationProviderFactory {
    @NotNull private final PsiClassFinder psiClassFinder;

    public PsiDeclarationProviderFactory(@NotNull PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @NotNull
    public ClassPsiDeclarationProvider createSyntheticClassObjectClassData(@NotNull PsiClass psiClass) {
        return createDeclarationProviderForClassStaticMembers(psiClass);
    }

    @NotNull
    public ClassPsiDeclarationProvider createBinaryClassData(@NotNull PsiClass psiClass) {
        return new ClassPsiDeclarationProviderImpl(psiClass, false, psiClassFinder);
    }

    @NotNull
    public PackagePsiDeclarationProvider createDeclarationProviderForNamespaceWithoutMembers(@NotNull PsiPackage psiPackage) {
        return new PackagePsiDeclarationProviderImpl(psiPackage, psiClassFinder);
    }

    @NotNull
    public ClassPsiDeclarationProvider createDeclarationProviderForClassStaticMembers(@NotNull PsiClass psiClass) {
        return new ClassPsiDeclarationProviderImpl(psiClass, true, psiClassFinder);
    }
}
