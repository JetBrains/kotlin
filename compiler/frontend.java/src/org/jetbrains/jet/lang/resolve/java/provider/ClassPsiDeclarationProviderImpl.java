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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;

public class ClassPsiDeclarationProviderImpl extends PsiDeclarationProviderBase implements ClassPsiDeclarationProvider {
    @NotNull
    protected final PsiClassFinder psiClassFinder;

    @NotNull
    private final PsiClass psiClass;

    private final boolean staticMembers;

    public ClassPsiDeclarationProviderImpl(@NotNull PsiClass psiClass, boolean staticMembers, @NotNull PsiClassFinder psiClassFinder) {
        this.staticMembers = staticMembers;
        this.psiClass = psiClass;
        this.psiClassFinder = psiClassFinder;
    }

    @Override
    @NotNull
    protected MembersCache buildMembersCache() {
        return MembersCache.buildMembersByNameCache(new MembersCache(), psiClassFinder, psiClass, null, staticMembers);
    }

    @Override
    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }

    @Override
    public boolean isStaticMembers() {
        return staticMembers;
    }
}
