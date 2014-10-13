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

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.ClassId;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.namedAnnotationArguments;

public class JavaAnnotationImpl extends JavaElementImpl<PsiAnnotation> implements JavaAnnotation {
    public JavaAnnotationImpl(@NotNull PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @Override
    @Nullable
    public JavaAnnotationArgument findArgument(@NotNull Name name) {
        PsiAnnotationMemberValue attribute = getPsi().findAttributeValue(name.asString());
        return attribute == null ? null : JavaAnnotationArgumentImpl.OBJECT$.create(attribute, name);
    }

    @Override
    @NotNull
    public Collection<JavaAnnotationArgument> getArguments() {
        return namedAnnotationArguments(getPsi().getParameterList().getAttributes());
    }

    @Override
    @Nullable
    public ClassId getClassId() {
        PsiClass resolved = resolvePsi();
        if (resolved != null) return computeClassId(resolved);

        // External annotations do not have PSI behind them,
        // so we can only heuristically reconstruct annotation class ids from qualified names
        String qualifiedName = getPsi().getQualifiedName();
        if (qualifiedName != null) return ClassId.topLevel(new FqName(qualifiedName));

        return null;
    }

    @Nullable
    @Override
    public JavaClass resolve() {
        PsiClass resolved = resolvePsi();
        return resolved == null ? null : new JavaClassImpl(resolved);
    }

    @Nullable
    private static ClassId computeClassId(@NotNull PsiClass psiClass) {
        PsiClass container = psiClass.getContainingClass();
        if (container != null) {
            ClassId parentClassId = computeClassId(container);
            return parentClassId == null ? null : parentClassId.createNestedClassId(Name.identifier(psiClass.getName()));
        }

        String fqName = psiClass.getQualifiedName();
        return fqName == null ? null : ClassId.topLevel(new FqName(fqName));
    }

    @Nullable
    private PsiClass resolvePsi() {
        PsiJavaCodeReferenceElement referenceElement = getPsi().getNameReferenceElement();
        if (referenceElement == null) return null;

        PsiElement resolved = referenceElement.resolve();
        return resolved instanceof PsiClass ? (PsiClass) resolved : null;
    }
}
