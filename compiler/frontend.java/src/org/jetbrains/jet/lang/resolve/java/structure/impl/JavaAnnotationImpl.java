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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.namedAnnotationArguments;

public class JavaAnnotationImpl extends JavaElementImpl implements JavaAnnotation {
    public JavaAnnotationImpl(@NotNull PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @NotNull
    @Override
    public PsiAnnotation getPsi() {
        return (PsiAnnotation) super.getPsi();
    }

    @Override
    @Nullable
    public JavaAnnotationArgument findArgument(@NotNull Name name) {
        PsiAnnotationMemberValue attribute = getPsi().findAttributeValue(name.asString());
        return attribute == null ? null : JavaAnnotationArgumentImpl.create(attribute, name);
    }

    @Override
    @NotNull
    public Collection<JavaAnnotationArgument> getArguments() {
        return namedAnnotationArguments(getPsi().getParameterList().getAttributes());
    }

    @Override
    @Nullable
    public FqName getFqName() {
        String qualifiedName = getPsi().getQualifiedName();
        return qualifiedName == null ? null : new FqName(qualifiedName);
    }
}
