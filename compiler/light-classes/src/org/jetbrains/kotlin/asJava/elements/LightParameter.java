/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.lang.Language;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

// Based on com.intellij.psi.impl.light.LightParameter
public class LightParameter extends LightVariableBuilder implements PsiParameter {
    public static final LightParameter[] EMPTY_ARRAY = new LightParameter[0];

    private final String myName;
    private final KtLightMethod myDeclarationScope;
    private final boolean myVarArgs;

    public LightParameter(@NotNull String name, @NotNull PsiType type, @NotNull KtLightMethod declarationScope, Language language) {
        this(name, type, declarationScope, language, type instanceof PsiEllipsisType);
    }

    public LightParameter(
            @NotNull String name,
            @NotNull PsiType type,
            @NotNull KtLightMethod declarationScope,
            Language language,
            boolean isVarArgs
    ) {
        super(declarationScope.getManager(), name, type, language);
        myName = name;
        myDeclarationScope = declarationScope;
        myVarArgs = isVarArgs;
    }

    @NotNull
    @Override
    public KtLightMethod getDeclarationScope() {
        return myDeclarationScope;
    }

    public KtLightMethod getMethod() {
        return myDeclarationScope;
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor) visitor).visitParameter(this);
        }
    }

    public String toString() {
        return "Light Parameter";
    }

    @Override
    public boolean isVarArgs() {
        return myVarArgs;
    }

    @Override
    @NotNull
    public String getName() {
        return myName;
    }
}
