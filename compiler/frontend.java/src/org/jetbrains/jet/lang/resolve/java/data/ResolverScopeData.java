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

package org.jetbrains.jet.lang.resolve.java.data;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.MembersCache;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.lang.resolve.java.data.Origin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.data.Origin.KOTLIN;

public abstract class ResolverScopeData implements ClassPsiDeclarationProvider {

    private MembersCache membersCache = null;

    private final PsiClass psiClass;

    @Nullable
    private final PsiPackage psiPackage;

    private final boolean staticMembers;

    @NotNull
    private final Origin origin;

    public ResolverScopeData(
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            @Nullable FqName fqName,
            boolean staticMembers
    ) {
        DescriptorResolverUtils.checkPsiClassIsNotJet(psiClass);

        this.psiClass = psiClass;
        this.psiPackage = psiPackage;

        if (psiClass == null && psiPackage == null) {
            throw new IllegalStateException("both psiClass and psiPackage cannot be null");
        }

        this.staticMembers = staticMembers;
        this.origin = determineOrigin(psiClass);

        //TODO: move check to remove fqName parameter
        if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS)) && psiClass != null && getOrigin() == KOTLIN) {
            throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
        }
    }

    @NotNull
    private static Origin determineOrigin(@Nullable PsiClass psiClass) {
        return ((psiClass != null) && DescriptorResolverUtils.isKotlinClass(psiClass)) ? KOTLIN : JAVA;
    }

    @NotNull
    public PsiElement getPsiPackageOrPsiClass() {
        if (psiPackage != null) {
            return psiPackage;
        }
        else {
            assert psiClass != null;
            return psiClass;
        }
    }
    @Override
    @NotNull
    public MembersCache getMembersCache() {
        if (membersCache == null) {
            membersCache = MembersCache.buildMembersByNameCache(psiClass, psiPackage, staticMembers, getOrigin() == KOTLIN);
        }
        return membersCache;
    }

    @Override
    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }

    @Override
    public boolean isEmpty() {
        return psiClass == null;
    }

    @Nullable
    public PsiPackage getPsiPackage() {
        return psiPackage;
    }

    @Override
    @NotNull
    public Origin getOrigin() {
        return origin;
    }

    public boolean isStaticMembers() {
        return staticMembers;
    }
}
