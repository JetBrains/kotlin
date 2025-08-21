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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.structure.JavaConstructor;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;

import java.util.List;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.typeParameters;
import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.valueParameters;

public class JavaConstructorImpl extends JavaMemberImpl<PsiMethod> implements JavaConstructor {
    public JavaConstructorImpl(@NotNull JavaElementPsiSource<PsiMethod> psiConstructorSource) {
        super(psiConstructorSource);
        PsiMethod psiMethod = psiConstructorSource.getPsi();
        assert psiMethod.isConstructor() :
                "PsiMethod which is not a constructor should not be wrapped in JavaConstructorImpl: " +
                psiMethod.getName() + " " + psiMethod.getClass().getName();
    }

    @SuppressWarnings("unused") // used in KSP
    public JavaConstructorImpl(PsiMethod psiMethod) {
        this(JavaElementSourceFactory.getInstance(psiMethod.getProject()).createPsiSource(psiMethod));
    }


    @NotNull
    @Override
    public List<JavaValueParameter> getValueParameters() {
        return valueParameters(getPsi().getParameterList().getParameters(), getSourceFactory());
    }

    @NotNull
    @Override
    public List<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters(), getSourceFactory());
    }
}
