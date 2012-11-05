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
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.MembersCache;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.lang.resolve.java.data.Origin.JAVA;

public class ResolverNamespaceData extends PsiDeclarationProviderBase implements PackagePsiDeclarationProvider {

    @NotNull
    public static PsiDeclarationProvider createDeclarationProviderForPackage(
            @Nullable PsiPackage psiPackage,
            @Nullable PsiClass psiClass,
            //TODO:
            @Nullable FqName fqName
    ) {
        if (psiClass == null) {

            assert psiPackage != null;
            return new ResolverNamespaceData(psiPackage);
        }
        if (psiPackage == null) {
            return new ResolverClassData(psiClass, true);
        }
        KotlinNamespacePsiDeclarationProvider result = new KotlinNamespacePsiDeclarationProvider(psiPackage, psiClass);
        if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS))) {
            throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
        }
        return result;
    }

    @NotNull
    private final PsiPackage psiPackage;

    protected ResolverNamespaceData(
            @NotNull PsiPackage psiPackage
    ) {
        this.psiPackage = psiPackage;
    }

    @NotNull
    @Override
    public PsiPackage getPsiPackage() {
        return psiPackage;
    }

    @NotNull
    @Override
    protected MembersCache buildMembersCache() {
        return MembersCache.buildMembersByNameCache(new MembersCache(), null, getPsiPackage(), true, false);
    }

    @NotNull
    @Override
    public Origin getOrigin() {
        return JAVA;
    }
}
