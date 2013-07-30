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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.*;

public class JavaClass extends JavaElementImpl implements JavaNamedElement, JavaModifierListOwner, JavaTypeParameterListOwner {
    public JavaClass(@NotNull PsiClass psiClass) {
        super(psiClass);
    }

    @NotNull
    @Override
    public PsiClass getPsi() {
        return (PsiClass) super.getPsi();
    }

    @NotNull
    public Collection<JavaClass> getInnerClasses() {
        return classes(getPsi().getInnerClasses());
    }

    @Nullable
    public FqName getFqName() {
        String qualifiedName = getPsi().getQualifiedName();
        return qualifiedName == null ? null : new FqName(qualifiedName);
    }

    @Nullable
    public JavaAnnotation findAnnotation(@NotNull String fqName) {
        PsiModifierList modifierList = getPsi().getModifierList();
        if (modifierList != null) {
            PsiAnnotation annotation = modifierList.findAnnotation(fqName);
            if (annotation != null) {
                return new JavaAnnotation(annotation);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Name getName() {
        return Name.identifier(getPsi().getName());
    }

    public boolean isInterface() {
        return getPsi().isInterface();
    }

    public boolean isAnnotationType() {
        return getPsi().isAnnotationType();
    }

    public boolean isEnum() {
        return getPsi().isEnum();
    }

    @NotNull
    public ClassKind getKind() {
        if (isInterface()) {
            return isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT;
        }
        return isEnum() ? ClassKind.ENUM_CLASS : ClassKind.CLASS;
    }

    @Nullable
    public JavaClass getOuterClass() {
        PsiClass outer = getPsi().getContainingClass();
        return outer == null ? null : new JavaClass(outer);
    }

    @NotNull
    public Modality getModality() {
        return isAnnotationType() ? Modality.FINAL : Modality.convertFromFlags(isAbstract() || isInterface(), !isFinal());
    }

    @Override
    public boolean isAbstract() {
        return getPsi().hasModifierProperty(PsiModifier.ABSTRACT);
    }

    @Override
    public boolean isFinal() {
        return getPsi().hasModifierProperty(PsiModifier.FINAL);
    }

    @Override
    public boolean isStatic() {
        return getPsi().hasModifierProperty(PsiModifier.STATIC);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        if (getPsi().hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.PUBLIC;
        }
        if (getPsi().hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.PRIVATE;
        }
        if (getPsi().hasModifierProperty(PsiModifier.PROTECTED)) {
            return isStatic() ? JavaVisibilities.PROTECTED_STATIC_VISIBILITY : JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        PsiModifierList modifierList = getPsi().getModifierList();
        if (modifierList != null) {
            return annotations(modifierList.getAnnotations());
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public Collection<JavaClassType> getSupertypes() {
        // TODO: getPsi().getSuperTypes() ?
        Collection<JavaClassType> superClasses = classTypes(getPsi().getExtendsListTypes());
        Collection<JavaClassType> superInterfaces = classTypes(getPsi().getImplementsListTypes());
        return ContainerUtil.collect(ContainerUtil.concat(superClasses, superInterfaces).iterator());
    }

    @NotNull
    public Collection<JavaMethod> getMethods() {
        return methods(getPsi().getMethods());
    }
}
