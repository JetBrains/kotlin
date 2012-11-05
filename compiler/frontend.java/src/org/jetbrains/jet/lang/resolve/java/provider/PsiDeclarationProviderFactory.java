/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public final class PsiDeclarationProviderFactory {
    private PsiDeclarationProviderFactory() {
    }

    @NotNull
    public static ClassPsiDeclarationProvider createSyntheticClassObjectClassData(
            @NotNull PsiClass psiClass
    ) {
        return createDeclarationProviderForClassStaticMembers(psiClass);
    }

    @NotNull
    public static ClassPsiDeclarationProvider createBinaryClassData(
            @NotNull PsiClass psiClass
    ) {
        return new ClassPsiDeclarationProviderImpl(psiClass, false);
    }

    @NotNull
    public static PsiDeclarationProvider createDeclarationProviderForPackage(
            @Nullable PsiPackage psiPackage,
            @Nullable PsiClass psiClass,
            //TODO: remove this parameter
            @Nullable FqName fqName
    ) {
        if (psiClass == null) {
            assert psiPackage != null;
            return createDeclarationProviderForNamespaceWithoutMembers(psiPackage);
        }
        if (psiPackage == null) {
            return createDeclarationProviderForClassStaticMembers(psiClass);
        }
        return createDeclarationForKotlinNamespace(psiPackage, psiClass, fqName);
    }

    @NotNull
    private static KotlinNamespacePsiDeclarationProvider createDeclarationForKotlinNamespace(
            @NotNull PsiPackage psiPackage,
            @NotNull PsiClass psiClass,
            @Nullable FqName fqName
    ) {
        KotlinNamespacePsiDeclarationProvider result = new KotlinNamespacePsiDeclarationProvider(psiPackage, psiClass);
        if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS))) {
            throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
        }
        return result;
    }

    @NotNull
    /*package private*/ static PackagePsiDeclarationProvider createDeclarationProviderForNamespaceWithoutMembers(@NotNull PsiPackage psiPackage) {
        return new PackagePsiDeclarationProviderImpl(psiPackage);
    }

    @NotNull
    private static ClassPsiDeclarationProvider createDeclarationProviderForClassStaticMembers(@NotNull PsiClass psiClass) {
        return new ClassPsiDeclarationProviderImpl(psiClass, true);
    }
}
