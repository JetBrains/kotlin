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
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.*;

public class JavaClass extends JavaClassifier implements JavaNamedElement, JavaTypeParameterListOwner, JavaModifierListOwner {
    public JavaClass(@NotNull PsiClass psiClass) {
        super(psiClass);
        assert !(psiClass instanceof PsiTypeParameter)
                : "PsiTypeParameter should be wrapped in JavaTypeParameter, not JavaClass: use JavaClassifier.create()";
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

    @NotNull
    public Collection<JavaMethod> getAllMethods() {
        return methods(getPsi().getAllMethods());
    }

    @NotNull
    public Collection<JavaField> getAllFields() {
        return fields(getPsi().getAllFields());
    }

    @NotNull
    public Collection<JavaMethod> getConstructors() {
        return methods(getPsi().getConstructors());
    }

    @Override
    public boolean isAbstract() {
        return JavaElementUtil.isAbstract(this);
    }

    @Override
    public boolean isStatic() {
        return JavaElementUtil.isStatic(this);
    }

    @Override
    public boolean isFinal() {
        return JavaElementUtil.isFinal(this);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return JavaElementUtil.getVisibility(this);
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        return JavaElementUtil.getAnnotations(this);
    }
}
