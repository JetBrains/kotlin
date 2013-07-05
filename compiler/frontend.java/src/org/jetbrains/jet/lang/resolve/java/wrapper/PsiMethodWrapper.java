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

package org.jetbrains.jet.lang.resolve.java.wrapper;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PsiMethodWrapper extends PsiMemberWrapper {

    public PsiMethodWrapper(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
    }

    private List<PsiParameter> parameters;

    @NotNull
    public List<PsiParameter> getParameters() {
        if (parameters == null) {
            parameters = Lists.newArrayList(getPsiMethod().getParameterList().getParameters());
        }
        return parameters;
    }

    @Override
    public boolean isAbstract() {
        return psiMember.hasModifierProperty(PsiModifier.ABSTRACT);
    }

    @NotNull
    public PsiMethod getPsiMethod() {
        return (PsiMethod) psiMember;
    }

    @Nullable
    public PsiType getReturnType() {
        return getPsiMethod().getReturnType();
    }
}
