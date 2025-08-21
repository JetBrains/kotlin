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
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;
import org.jetbrains.kotlin.name.FqName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.annotations;
import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.nullabilityAnnotations;

/* package */ class JavaElementUtil {
    private JavaElementUtil() {
    }

    public static boolean isAbstract(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.ABSTRACT);
    }

    public static boolean isStatic(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.STATIC);
    }

    public static boolean isFinal(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.FINAL);
    }

    public static boolean isSealed(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.SEALED);
    }

    @NotNull
    public static Visibility getVisibility(@NotNull JavaModifierListOwnerImpl owner) {
        PsiModifierListOwner psiOwner = owner.getPsi();
        if (psiOwner.hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.Public.INSTANCE;
        }
        if (psiOwner.hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.Private.INSTANCE;
        }
        if (psiOwner.hasModifierProperty(PsiModifier.PROTECTED)) {
            return owner.isStatic() ? JavaVisibilities.ProtectedStaticVisibility.INSTANCE : JavaVisibilities.ProtectedAndPackage.INSTANCE;
        }
        return JavaVisibilities.PackageVisibility.INSTANCE;
    }

    @NotNull
    public static Collection<JavaAnnotation> getAnnotations(@NotNull JavaAnnotationOwnerImpl owner, JavaElementSourceFactory sourceFactory) {
        PsiAnnotationOwner annotationOwnerPsi = owner.getAnnotationOwnerPsi();
        if (annotationOwnerPsi != null) {
            return annotations(annotationOwnerPsi.getAnnotations(), sourceFactory);
        }
        return Collections.emptyList();
    }

    @Nullable
    private static PsiAnnotation[] getExternalAnnotations(@NotNull JavaModifierListOwnerImpl modifierListOwner) {
        PsiModifierListOwner psiModifierListOwner = modifierListOwner.getPsi();
        ExternalAnnotationsManager externalAnnotationManager = ExternalAnnotationsManager
                .getInstance(psiModifierListOwner.getProject());
        return externalAnnotationManager.findExternalAnnotations(psiModifierListOwner);
    }

    @NotNull
    static <T extends JavaAnnotationOwnerImpl & JavaModifierListOwnerImpl>
    Collection<JavaAnnotation> getRegularAndExternalAnnotations(@NotNull T owner, JavaElementSourceFactory sourceFactory) {
        PsiAnnotation[] externalAnnotations = getExternalAnnotations(owner);
        if (externalAnnotations == null) {
            return getAnnotations(owner, sourceFactory);
        }
        Collection<JavaAnnotation> annotations = new ArrayList<>(getAnnotations(owner, sourceFactory));
        annotations.addAll(nullabilityAnnotations(externalAnnotations, sourceFactory));
        return annotations;
    }


    @Nullable
    public static JavaAnnotation findAnnotation(@NotNull JavaAnnotationOwnerImpl owner, @NotNull FqName fqName, JavaElementSourceFactory sourceFactory) {
        PsiAnnotationOwner annotationOwnerPsi = owner.getAnnotationOwnerPsi();
        if (annotationOwnerPsi != null) {
            PsiAnnotation psiAnnotation = annotationOwnerPsi.findAnnotation(fqName.asString());
            return psiAnnotation == null ? null : new JavaAnnotationImpl(sourceFactory.createPsiSource(psiAnnotation));
        }
        return null;
    }
}
