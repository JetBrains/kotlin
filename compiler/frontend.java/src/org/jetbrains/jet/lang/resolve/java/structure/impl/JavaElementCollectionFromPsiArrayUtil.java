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
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaElementCollectionFromPsiArrayUtil {
    private JavaElementCollectionFromPsiArrayUtil() {
    }

    private interface Factory<Psi, Java> {
        @NotNull
        Java create(@NotNull Psi psi);
    }

    private static class Factories {
        private static final Factory<PsiClass, JavaClass> CLASSES = new Factory<PsiClass, JavaClass>() {
            @NotNull
            @Override
            public JavaClass create(@NotNull PsiClass psiClass) {
                return new JavaClassImpl(psiClass);
            }
        };

        private static final Factory<PsiPackage, JavaPackage> PACKAGES = new Factory<PsiPackage, JavaPackage>() {
            @NotNull
            @Override
            public JavaPackage create(@NotNull PsiPackage psiPackage) {
                return new JavaPackageImpl(psiPackage);
            }
        };

        private static final Factory<PsiMethod, JavaMethod> METHODS = new Factory<PsiMethod, JavaMethod>() {
            @NotNull
            @Override
            public JavaMethod create(@NotNull PsiMethod psiMethod) {
                return new JavaMethodImpl(psiMethod);
            }
        };

        private static final Factory<PsiField, JavaField> FIELDS = new Factory<PsiField, JavaField>() {
            @NotNull
            @Override
            public JavaField create(@NotNull PsiField psiField) {
                return new JavaFieldImpl(psiField);
            }
        };

        private static final Factory<PsiParameter, JavaValueParameter> VALUE_PARAMETERS = new Factory<PsiParameter, JavaValueParameter>() {
            @NotNull
            @Override
            public JavaValueParameter create(@NotNull PsiParameter psiParameter) {
                return new JavaValueParameterImpl(psiParameter);
            }
        };

        private static final Factory<PsiTypeParameter, JavaTypeParameter> TYPE_PARAMETERS =
                new Factory<PsiTypeParameter, JavaTypeParameter>() {
            @NotNull
            @Override
            public JavaTypeParameter create(@NotNull PsiTypeParameter psiTypeParameter) {
                return new JavaTypeParameterImpl(psiTypeParameter);
            }
        };

        private static final Factory<PsiType, JavaType> TYPES = new Factory<PsiType, JavaType>() {
            @NotNull
            @Override
            public JavaType create(@NotNull PsiType psiType) {
                return JavaTypeImpl.create(psiType);
            }
        };

        private static final Factory<PsiClassType, JavaClassifierType> CLASSIFIER_TYPES = new Factory<PsiClassType, JavaClassifierType>() {
            @NotNull
            @Override
            public JavaClassifierType create(@NotNull PsiClassType psiClassType) {
                return new JavaClassifierTypeImpl(psiClassType);
            }
        };

        private static final Factory<PsiAnnotation, JavaAnnotation> ANNOTATIONS = new Factory<PsiAnnotation, JavaAnnotation>() {
            @NotNull
            @Override
            public JavaAnnotation create(@NotNull PsiAnnotation psiAnnotation) {
                return new JavaAnnotationImpl(psiAnnotation);
            }
        };

        private static final Factory<PsiAnnotationMemberValue, JavaAnnotationArgument> NAMELESS_ANNOTATION_ARGUMENTS =
                new Factory<PsiAnnotationMemberValue, JavaAnnotationArgument>() {
            @NotNull
            @Override
            public JavaAnnotationArgument create(@NotNull PsiAnnotationMemberValue psiAnnotationMemberValue) {
                return JavaAnnotationArgumentImpl.create(psiAnnotationMemberValue, null);
            }
        };

        private static final Factory<PsiNameValuePair, JavaAnnotationArgument> NAMED_ANNOTATION_ARGUMENTS =
                new Factory<PsiNameValuePair, JavaAnnotationArgument>() {
            @NotNull
            @Override
            public JavaAnnotationArgument create(@NotNull PsiNameValuePair psiNameValuePair) {
                String name = psiNameValuePair.getName();
                PsiAnnotationMemberValue value = psiNameValuePair.getValue();
                assert value != null : "Annotation argument value cannot be null: " + name;
                return JavaAnnotationArgumentImpl.create(value, name == null ? null : Name.identifier(name));
            }
        };
    }

    @NotNull
    private static <Psi, Java> List<Java> convert(@NotNull Psi[] elements, @NotNull Factory<Psi, Java> factory) {
        if (elements.length == 0) return Collections.emptyList();
        List<Java> result = new ArrayList<Java>(elements.length);
        for (Psi element : elements) {
            result.add(factory.create(element));
        }
        return result;
    }

    @NotNull
    public static Collection<JavaClass> classes(@NotNull PsiClass[] classes) {
        return convert(classes, Factories.CLASSES);
    }

    @NotNull
    public static Collection<JavaPackage> packages(@NotNull PsiPackage[] packages) {
        return convert(packages, Factories.PACKAGES);
    }

    @NotNull
    public static Collection<JavaMethod> methods(@NotNull PsiMethod[] methods) {
        return convert(methods, Factories.METHODS);
    }

    @NotNull
    public static Collection<JavaField> fields(@NotNull PsiField[] fields) {
        return convert(fields, Factories.FIELDS);
    }

    @NotNull
    public static List<JavaValueParameter> valueParameters(@NotNull PsiParameter[] parameters) {
        return convert(parameters, Factories.VALUE_PARAMETERS);
    }

    @NotNull
    public static List<JavaTypeParameter> typeParameters(@NotNull PsiTypeParameter[] typeParameters) {
        return convert(typeParameters, Factories.TYPE_PARAMETERS);
    }

    @NotNull
    public static List<JavaType> types(@NotNull PsiType[] types) {
        return convert(types, Factories.TYPES);
    }

    @NotNull
    public static Collection<JavaClassifierType> classifierTypes(@NotNull PsiClassType[] classTypes) {
        return convert(classTypes, Factories.CLASSIFIER_TYPES);
    }

    @NotNull
    public static Collection<JavaAnnotation> annotations(@NotNull PsiAnnotation[] annotations) {
        return convert(annotations, Factories.ANNOTATIONS);
    }

    @NotNull
    public static List<JavaAnnotationArgument> namelessAnnotationArguments(@NotNull PsiAnnotationMemberValue[] memberValues) {
        return convert(memberValues, Factories.NAMELESS_ANNOTATION_ARGUMENTS);
    }

    @NotNull
    public static Collection<JavaAnnotationArgument> namedAnnotationArguments(@NotNull PsiNameValuePair[] nameValuePairs) {
        return convert(nameValuePairs, Factories.NAMED_ANNOTATION_ARGUMENTS);
    }
}
