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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

public class TypeSource {

    @NotNull
    private final String typeString;
    @NotNull
    private final PsiType psiType;
    @NotNull
    private final PsiModifierListOwner psiNotNullOwner;

    public TypeSource(@NotNull String typeString, @NotNull PsiType psiType, @NotNull PsiModifierListOwner psiNotNullOwner) {
        this.typeString = typeString;
        this.psiType = psiType;
        this.psiNotNullOwner = psiNotNullOwner;
    }

    @NotNull
    public String getTypeString() {
        return typeString;
    }

    @NotNull
    public PsiType getPsiType() {
        return psiType;
    }

    @NotNull
    public PsiModifierListOwner getPsiNotNullOwner() {
        return psiNotNullOwner;
    }
}
