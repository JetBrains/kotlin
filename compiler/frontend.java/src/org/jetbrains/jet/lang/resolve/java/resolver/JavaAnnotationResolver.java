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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class JavaAnnotationResolver {
    private JavaClassResolver classResolver;
    private JavaCompileTimeConstResolver compileTimeConstResolver;

    public JavaAnnotationResolver() {
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setCompileTimeConstResolver(JavaCompileTimeConstResolver compileTimeConstResolver) {
        this.compileTimeConstResolver = compileTimeConstResolver;
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull PsiModifierListOwner owner) {
        PsiAnnotation[] psiAnnotations = getAllAnnotations(owner);
        List<AnnotationDescriptor> r = Lists.newArrayListWithCapacity(psiAnnotations.length);
        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            AnnotationDescriptor annotation = resolveAnnotation(psiAnnotation);
            if (annotation != null) {
                r.add(annotation);
            }
        }
        return r;
    }

    @Nullable
    public AnnotationDescriptor resolveAnnotation(PsiAnnotation psiAnnotation) {
        final AnnotationDescriptor annotation = new AnnotationDescriptor();
        String qname = psiAnnotation.getQualifiedName();
        if (qname == null) {
            return null;
        }

        // Don't process internal jet annotations and jetbrains NotNull annotations
        if (qname.startsWith("jet.runtime.typeinfo.") || qname.equals(JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName())) {
            return null;
        }

        FqName annotationFqName = new FqName(qname);

        AnnotationDescriptor mappedClassDescriptor = JavaToKotlinClassMap.getInstance().mapToAnnotationClass(annotationFqName);
        if (mappedClassDescriptor != null) {
            return mappedClassDescriptor;
        }
        PsiClass annotationPsiClass = getAnnotationPsiClass(psiAnnotation);
        if (annotationPsiClass == null) return null;

        final ClassDescriptor annotationClass =
                classResolver.resolveClass(annotationPsiClass, DescriptorSearchRule.INCLUDE_KOTLIN);
        if (annotationClass == null) {
            return null;
        }

        annotation.setAnnotationType(DeferredType.create(BindingTrace.EMPTY, new RecursionIntolerantLazyValue<JetType>() {
            @Override
            protected JetType compute() {
                return annotationClass.getDefaultType();
            }
        }));

        PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
        for (PsiNameValuePair psiNameValuePair : parameterList.getAttributes()) {
            PsiAnnotationMemberValue value = psiNameValuePair.getValue();
            String name = psiNameValuePair.getName();
            if (name == null) name = "value";
            Name identifier = Name.identifier(name);

            assert value != null;
            CompileTimeConstant compileTimeConst =
                    compileTimeConstResolver.getCompileTimeConstFromExpression(annotationFqName, identifier, value);
            if (compileTimeConst != null) {
                ValueParameterDescriptor valueParameterDescriptor =
                        DescriptorResolverUtils.getValueParameterDescriptorForAnnotationParameter(identifier, annotationClass);
                if (valueParameterDescriptor != null) {
                    annotation.setValueArgument(valueParameterDescriptor, compileTimeConst);
                }
            }
        }

        return annotation;
    }

    @Nullable
    private static PsiClass getAnnotationPsiClass(@NotNull PsiAnnotation psiAnnotation) {
        PsiJavaCodeReferenceElement nameReferenceElement = psiAnnotation.getNameReferenceElement();
        if (nameReferenceElement == null) return null;
        PsiElement annotationResolvedTo = nameReferenceElement.resolve();
        if (annotationResolvedTo == null) return null;
        assert annotationResolvedTo instanceof PsiClass : "Annotation " + psiAnnotation.getText() +
                                                        " resolved to something other than a class: " + annotationResolvedTo.getText();
        return (PsiClass) annotationResolvedTo;
    }

    @NotNull
    private static PsiAnnotation[] getAllAnnotations(@NotNull PsiModifierListOwner owner) {
        List<PsiAnnotation> result = new ArrayList<PsiAnnotation>();

        PsiModifierList list = owner.getModifierList();
        if (list != null) {
            result.addAll(Arrays.asList(list.getAnnotations()));
        }

        PsiAnnotation[] externalAnnotations = ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotations(owner);
        if (externalAnnotations != null) {
            result.addAll(Arrays.asList(externalAnnotations));
        }

        return result.toArray(new PsiAnnotation[result.size()]);
    }

    @Nullable
    public static PsiAnnotation findOwnAnnotation(@NotNull PsiModifierListOwner owner, @NotNull String fqName) {
        PsiModifierList list = owner.getModifierList();
        if (list != null) {
            PsiAnnotation found = list.findAnnotation(fqName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    @Nullable
    public static PsiAnnotation findAnnotationWithExternal(@NotNull PsiModifierListOwner owner, @NotNull String fqName) {
        PsiAnnotation annotation = findOwnAnnotation(owner, fqName);
        if (annotation != null) {
            return annotation;
        }

        return ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotation(owner, fqName);
    }
}