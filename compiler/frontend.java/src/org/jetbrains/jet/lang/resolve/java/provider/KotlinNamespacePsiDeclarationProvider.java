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

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class KotlinNamespacePsiDeclarationProvider extends ClassPsiDeclarationProviderImpl implements PackagePsiDeclarationProvider {

    @NotNull
    private final PackagePsiDeclarationProvider packagePsiDeclarationProvider;

    public KotlinNamespacePsiDeclarationProvider(
            @NotNull PsiPackage psiPackage,
            @NotNull PsiClass psiClass
    ) {
        super(psiClass, true);
        this.packagePsiDeclarationProvider = PsiDeclarationProviderFactory.createDeclarationProviderForNamespaceWithoutMembers(psiPackage);
    }

    @NotNull
    @Override
    public PsiPackage getPsiPackage() {
        return packagePsiDeclarationProvider.getPsiPackage();
    }

    @NotNull
    @Override
    protected MembersCache buildMembersCache() {
        MembersCache cacheWithMembers = super.buildMembersCache();
        MembersCache.buildMembersByNameCache(cacheWithMembers, null, packagePsiDeclarationProvider.getPsiPackage(), true, getDeclarationOrigin() == KOTLIN);
        return cacheWithMembers;
    }

    @NotNull
    @Override
    public DeclarationOrigin getDeclarationOrigin() {
        return KOTLIN;
    }
}
