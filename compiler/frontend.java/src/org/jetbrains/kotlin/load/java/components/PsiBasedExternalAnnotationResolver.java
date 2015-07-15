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

package org.jetbrains.kotlin.load.java.components;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner;
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl;
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil;
import org.jetbrains.kotlin.load.java.structure.impl.JavaModifierListOwnerImpl;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;
import java.util.Collections;

public class PsiBasedExternalAnnotationResolver implements ExternalAnnotationResolver {
    @Nullable
    @Override
    public JavaAnnotation findExternalAnnotation(@NotNull JavaAnnotationOwner owner, @NotNull FqName fqName) {
        if (owner instanceof JavaModifierListOwnerImpl) {
            JavaModifierListOwnerImpl modifierListOwner = (JavaModifierListOwnerImpl) owner;
            PsiAnnotation psiAnnotation = findExternalAnnotation(modifierListOwner.getPsi(), fqName);
            return psiAnnotation == null ? null : new JavaAnnotationImpl(psiAnnotation);
        }
        return null;
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> findExternalAnnotations(@NotNull JavaAnnotationOwner owner) {
        if (owner instanceof JavaModifierListOwnerImpl) {
            PsiModifierListOwner psiOwner = ((JavaModifierListOwnerImpl) owner).getPsi();
            PsiAnnotation[] annotations = ExternalAnnotationsManager.getInstance(psiOwner.getProject()).findExternalAnnotations(psiOwner);
            return annotations == null
                   ? Collections.<JavaAnnotation>emptyList()
                   : JavaElementCollectionFromPsiArrayUtil.annotations(annotations);
        }
        return Collections.emptyList();
    }

    @Nullable
    public static PsiAnnotation findExternalAnnotation(@NotNull PsiModifierListOwner owner, @NotNull FqName fqName) {
        return ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotation(owner, fqName.asString());
    }
}
