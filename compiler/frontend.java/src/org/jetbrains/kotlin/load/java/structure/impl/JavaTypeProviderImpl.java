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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeProvider;
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType;

public class JavaTypeProviderImpl implements JavaTypeProvider {
    private final PsiManager manager;

    public JavaTypeProviderImpl(@NotNull PsiManager manager) {
        this.manager = manager;
    }

    @Override
    @NotNull
    public JavaType createJavaLangObjectType() {
        return JavaTypeImpl.create(PsiType.getJavaLangObject(manager, GlobalSearchScope.allScope(manager.getProject())));
    }

    @NotNull
    @Override
    public JavaWildcardType createUpperBoundWildcard(@NotNull JavaType bound) {
        return new JavaWildcardTypeImpl(PsiWildcardType.createExtends(manager, ((JavaTypeImpl) bound).getPsi()));
    }

    @NotNull
    @Override
    public JavaWildcardType createLowerBoundWildcard(@NotNull JavaType bound) {
        return new JavaWildcardTypeImpl(PsiWildcardType.createSuper(manager, ((JavaTypeImpl) bound).getPsi()));
    }

    @NotNull
    @Override
    public JavaWildcardType createUnboundedWildcard() {
        return new JavaWildcardTypeImpl(PsiWildcardType.createUnbounded(manager));
    }
}
