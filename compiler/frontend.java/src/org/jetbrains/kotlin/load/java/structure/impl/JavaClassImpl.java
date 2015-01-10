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

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiSubstitutorImpl;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.JetJavaMirrorMarker;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.load.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.*;

public class JavaClassImpl extends JavaClassifierImpl<PsiClass> implements JavaClass, JavaAnnotationOwnerImpl, JavaModifierListOwnerImpl {
    public JavaClassImpl(@NotNull PsiClass psiClass) {
        super(psiClass);
        assert !(psiClass instanceof PsiTypeParameter)
                : "PsiTypeParameter should be wrapped in JavaTypeParameter, not JavaClass: use JavaClassifier.create()";
    }

    @Override
    @NotNull
    public Collection<JavaClass> getInnerClasses() {
        return classes(getPsi().getInnerClasses());
    }

    @Override
    @Nullable
    public FqName getFqName() {
        String qualifiedName = getPsi().getQualifiedName();
        return qualifiedName == null ? null : new FqName(qualifiedName);
    }

    @NotNull
    @Override
    public Name getName() {
        return Name.identifier(getPsi().getName());
    }

    @Override
    public boolean isInterface() {
        return getPsi().isInterface();
    }

    @Override
    public boolean isAnnotationType() {
        return getPsi().isAnnotationType();
    }

    @Override
    public boolean isEnum() {
        return getPsi().isEnum();
    }

    @Override
    @Nullable
    public JavaClass getOuterClass() {
        PsiClass outer = getPsi().getContainingClass();
        return outer == null ? null : new JavaClassImpl(outer);
    }

    @NotNull
    @Override
    public List<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getPsi().getTypeParameters());
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        return classifierTypes(getPsi().getSuperTypes());
    }

    @Override
    @NotNull
    public Collection<JavaMethod> getMethods() {
        return methods(KotlinPackage.filter(getPsi().getMethods(), new Function1<PsiMethod, Boolean>() {
            @Override
            public Boolean invoke(PsiMethod method) {
                return !method.isConstructor();
            }
        }));
    }

    @Override
    @NotNull
    public Collection<JavaField> getFields() {
        return fields(getPsi().getFields());
    }

    @Override
    @NotNull
    public Collection<JavaConstructor> getConstructors() {
        return constructors(getPsi().getConstructors());
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

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName);
    }

    @Override
    @NotNull
    public JavaClassifierType getDefaultType() {
        return new JavaClassifierTypeImpl(JavaPsiFacade.getElementFactory(getPsi().getProject()).createType(getPsi()));
    }

    @Override
    @NotNull
    public OriginKind getOriginKind() {
        PsiClass psiClass = getPsi();
        if (psiClass instanceof JetJavaMirrorMarker) {
            return OriginKind.KOTLIN_LIGHT_CLASS;
        }
        else if (psiClass instanceof PsiCompiledElement) {
            return OriginKind.COMPILED;
        }
        else {
            return OriginKind.SOURCE;
        }
    }

    @NotNull
    @Override
    public JavaType createImmediateType(@NotNull JavaTypeSubstitutor substitutor) {
        return new JavaClassifierTypeImpl(
                JavaPsiFacade.getElementFactory(getPsi().getProject()).createType(getPsi(), createPsiSubstitutor(substitutor)));
    }

    @NotNull
    private static PsiSubstitutor createPsiSubstitutor(@NotNull JavaTypeSubstitutor substitutor) {
        Map<PsiTypeParameter, PsiType> substMap = new HashMap<PsiTypeParameter, PsiType>();
        for (Map.Entry<JavaTypeParameter, JavaType> entry : substitutor.getSubstitutionMap().entrySet()) {
            PsiTypeParameter key = ((JavaTypeParameterImpl) entry.getKey()).getPsi();
            if (entry.getValue() == null) {
                substMap.put(key, null);
            }
            else {
                substMap.put(key, ((JavaTypeImpl) entry.getValue()).getPsi());
            }
        }

        return PsiSubstitutorImpl.createSubstitutor(substMap);
    }
}
