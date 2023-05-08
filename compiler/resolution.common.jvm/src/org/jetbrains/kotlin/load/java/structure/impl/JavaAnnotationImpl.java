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

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;
import java.util.Objects;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.namedAnnotationArguments;

public class JavaAnnotationImpl extends JavaElementImpl<PsiAnnotation> implements JavaAnnotation {
    public JavaAnnotationImpl(@NotNull JavaElementPsiSource<PsiAnnotation> psiAnnotation) {
        super(psiAnnotation);
    }

    @Override
    @NotNull
    public Collection<JavaAnnotationArgument> getArguments() {
        return namedAnnotationArguments(getPsi().getParameterList().getAttributes(), getSourceFactory());
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
        return resolved == null ? null : new JavaClassImpl(createPsiSource(resolved));
    }

    @Nullable
    private static ClassId computeClassId(@NotNull PsiClass psiClass) {
        PsiClass container = psiClass.getContainingClass();
        if (container != null) {
            ClassId parentClassId = computeClassId(container);
            String name = psiClass.getName();
            return parentClassId == null || name == null ? null : parentClassId.createNestedClassId(Name.identifier(name));
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

    @Override
    public boolean isIdeExternalAnnotation() {
        PsiAnnotation psi = getPsi();
        ExternalAnnotationsManager externalAnnotationManager = ExternalAnnotationsManager.getInstance(psi.getProject());
        return externalAnnotationManager.isExternalAnnotation(psi);
    }

    @Override
    public boolean isResolvedTo(@NotNull FqName fqName) {
        PsiJavaCodeReferenceElement referenceElement = getPsi().getNameReferenceElement();
        if (referenceElement == null || !Objects.equals(referenceElement.getReferenceName(), fqName.shortNameOrSpecial().asString())) {
            return false;
        }
        return getPsi().hasQualifiedName(fqName.asString());
    }

    @Override
    public boolean isFreshlySupportedTypeUseAnnotation() {
        return false;
    }
}
