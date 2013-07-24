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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.java.provider.MembersCache;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

/* package */ class MembersProvider {
    @NotNull
    private final PsiClassFinder psiClassFinder;
    @Nullable
    private final PsiClass psiClass;
    @Nullable
    private final PsiPackage psiPackage;
    private final boolean staticMembers;

    private MembersProvider(
            @NotNull PsiClassFinder psiClassFinder,
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            boolean staticMembers
    ) {
        this.psiClassFinder = psiClassFinder;
        this.psiClass = psiClass;
        this.psiPackage = psiPackage;
        this.staticMembers = staticMembers;
    }

    @NotNull
    public static MembersProvider forPackage(@NotNull PsiClassFinder psiClassFinder, @NotNull PsiPackage psiPackage) {
        return new MembersProvider(psiClassFinder, null, psiPackage, true);
    }

    @NotNull
    public static MembersProvider forClass(@NotNull PsiClassFinder psiClassFinder, @NotNull PsiClass psiClass, boolean staticMembers) {
        return new MembersProvider(psiClassFinder, psiClass, null, staticMembers);
    }

    @Nullable
    public NamedMembers get(@NotNull Name name) {
        return getMembersCache().get(name);
    }

    @NotNull
    public Collection<NamedMembers> allMembers() {
        return getMembersCache().allMembers();
    }

    private MembersCache membersCache;
    @NotNull
    private MembersCache getMembersCache() {
        if (membersCache == null) {
            membersCache = MembersCache.buildMembersByNameCache(psiClassFinder, psiClass, psiPackage, staticMembers);
        }
        return membersCache;
    }
}
