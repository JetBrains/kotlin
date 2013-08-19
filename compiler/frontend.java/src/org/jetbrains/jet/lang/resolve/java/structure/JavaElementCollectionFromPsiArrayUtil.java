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

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaElementCollectionFromPsiArrayUtil {
    private JavaElementCollectionFromPsiArrayUtil() {
    }

    @NotNull
    public static Collection<JavaClass> classes(@NotNull PsiClass[] classes) {
        if (classes.length == 0) return Collections.emptyList();
        List<JavaClass> result = new ArrayList<JavaClass>(classes.length);
        for (PsiClass psiClass : classes) {
            result.add(new JavaClass(psiClass));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaPackage> packages(@NotNull PsiPackage[] packages) {
        if (packages.length == 0) return Collections.emptyList();
        List<JavaPackage> result = new ArrayList<JavaPackage>(packages.length);
        for (PsiPackage psiPackage : packages) {
            result.add(new JavaPackageImpl(psiPackage));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaMethod> methods(@NotNull PsiMethod[] methods) {
        if (methods.length == 0) return Collections.emptyList();
        List<JavaMethod> result = new ArrayList<JavaMethod>(methods.length);
        for (PsiMethod psiMethod : methods) {
            result.add(new JavaMethodImpl(psiMethod));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaField> fields(@NotNull PsiField[] fields) {
        if (fields.length == 0) return Collections.emptyList();
        List<JavaField> result = new ArrayList<JavaField>(fields.length);
        for (PsiField psiField : fields) {
            result.add(new JavaFieldImpl(psiField));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaValueParameter> valueParameters(@NotNull PsiParameter[] parameters) {
        if (parameters.length == 0) return Collections.emptyList();
        List<JavaValueParameter> result = new ArrayList<JavaValueParameter>(parameters.length);
        for (PsiParameter psiParameter : parameters) {
            result.add(new JavaValueParameterImpl(psiParameter));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaTypeParameter> typeParameters(@NotNull PsiTypeParameter[] typeParameters) {
        if (typeParameters.length == 0) return Collections.emptyList();
        List<JavaTypeParameter> result = new ArrayList<JavaTypeParameter>(typeParameters.length);
        for (PsiTypeParameter psiTypeParameter : typeParameters) {
            result.add(new JavaTypeParameterImpl(psiTypeParameter));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaType> types(@NotNull PsiType[] types) {
        if (types.length == 0) return Collections.emptyList();
        List<JavaType> result = new ArrayList<JavaType>(types.length);
        for (PsiType psiType : types) {
            result.add(JavaTypeImpl.create(psiType));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaClassifierType> classifierTypes(@NotNull PsiClassType[] classTypes) {
        if (classTypes.length == 0) return Collections.emptyList();
        List<JavaClassifierType> result = new ArrayList<JavaClassifierType>(classTypes.length);
        for (PsiClassType psiClassType : classTypes) {
            result.add(new JavaClassifierTypeImpl(psiClassType));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaAnnotation> annotations(@NotNull PsiAnnotation[] annotations) {
        if (annotations.length == 0) return Collections.emptyList();
        List<JavaAnnotation> result = new ArrayList<JavaAnnotation>(annotations.length);
        for (PsiAnnotation psiAnnotation : annotations) {
            result.add(new JavaAnnotationImpl(psiAnnotation));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaAnnotationArgument> namelessAnnotationArguments(@NotNull PsiAnnotationMemberValue[] memberValues) {
        if (memberValues.length == 0) return Collections.emptyList();
        List<JavaAnnotationArgument> result = new ArrayList<JavaAnnotationArgument>(memberValues.length);
        for (PsiAnnotationMemberValue psiAnnotationMemberValue : memberValues) {
            result.add(JavaAnnotationArgumentImpl.create(psiAnnotationMemberValue, null));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaAnnotationArgument> namedAnnotationArguments(@NotNull PsiNameValuePair[] nameValuePairs) {
        if (nameValuePairs.length == 0) return Collections.emptyList();
        List<JavaAnnotationArgument> result = new ArrayList<JavaAnnotationArgument>(nameValuePairs.length);
        for (PsiNameValuePair pair : nameValuePairs) {
            String name = pair.getName();
            PsiAnnotationMemberValue value = pair.getValue();
            assert value != null : "Annotation argument value cannot be null: " + name;
            result.add(JavaAnnotationArgumentImpl.create(value, name == null ? null : Name.identifier(name)));
        }
        return result;
    }
}
