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

import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource;

public class JavaWildcardTypeImpl extends JavaTypeImpl<PsiWildcardType> implements JavaWildcardType {
    public JavaWildcardTypeImpl(@NotNull JavaElementTypeSource<PsiWildcardType> psiWildcardTypeSource) {
        super(psiWildcardTypeSource);
    }

    @Override
    @Nullable
    public JavaTypeImpl<?> getBound() {
        PsiType bound = getPsi().getBound();
        return bound == null ? null : create(createTypeSource(bound));
    }

    @Override
    public boolean isExtends() {
        return getPsi().isExtends();
    }
}
