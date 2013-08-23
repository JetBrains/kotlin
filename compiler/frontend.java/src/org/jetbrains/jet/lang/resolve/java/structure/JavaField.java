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

package org.jetbrains.jet.lang.resolve.java.structure;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaField extends JavaMemberImpl {
    public JavaField(@NotNull PsiField psiField) {
        super(psiField);
    }

    @NotNull
    @Override
    public PsiField getPsi() {
        return (PsiField) super.getPsi();
    }

    @Nullable
    @Override
    public JavaClass getContainingClass() {
        PsiClass psiClass = getPsi().getContainingClass();
        return psiClass == null ? null : new JavaClass(psiClass);
    }

    public boolean isEnumEntry() {
        return getPsi() instanceof PsiEnumConstant;
    }
}
