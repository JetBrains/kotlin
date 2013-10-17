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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationOwner;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public final class JavaAnnotationResolver {
    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");
    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = fqNameByClass(NotNull.class);
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");

    private JavaClassResolver classResolver;
    private JavaAnnotationArgumentResolver argumentResolver;
    private ExternalAnnotationResolver externalAnnotationResolver;

    public JavaAnnotationResolver() {
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setArgumentResolver(JavaAnnotationArgumentResolver argumentResolver) {
        this.argumentResolver = argumentResolver;
    }

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    private void resolveAnnotations(
            @NotNull Collection<JavaAnnotation> annotations,
            @NotNull PostponedTasks tasks,
            @NotNull List<AnnotationDescriptor> result
    ) {
        for (JavaAnnotation javaAnnotation : annotations) {
            AnnotationDescriptor annotation = resolveAnnotation(javaAnnotation, tasks);
            if (annotation != null) {
                result.add(annotation);
            }
        }
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JavaAnnotationOwner owner, @NotNull PostponedTasks tasks) {
        List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();
        resolveAnnotations(owner.getAnnotations(), tasks, result);
        resolveAnnotations(externalAnnotationResolver.findExternalAnnotations(owner), tasks, result);
        return result;
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JavaAnnotationOwner owner) {
        PostponedTasks postponedTasks = new PostponedTasks();
        List<AnnotationDescriptor> annotations = resolveAnnotations(owner, postponedTasks);
        postponedTasks.performTasks();
        return annotations;
    }

    @Nullable
    public AnnotationDescriptor resolveAnnotation(@NotNull JavaAnnotation javaAnnotation, @NotNull PostponedTasks postponedTasks) {
        final AnnotationDescriptorImpl annotation = new AnnotationDescriptorImpl();
        FqName fqName = javaAnnotation.getFqName();
        if (fqName == null) {
            return null;
        }

        // Don't process internal jet annotations and jetbrains NotNull annotations
        if (fqName.asString().startsWith("jet.runtime.typeinfo.")
            || fqName.equals(JETBRAINS_NOT_NULL_ANNOTATION)
            || fqName.equals(JvmAnnotationNames.KOTLIN_CLASS)
            || fqName.equals(JvmAnnotationNames.KOTLIN_PACKAGE)
        ) {
            return null;
        }

        AnnotationDescriptor mappedClassDescriptor = JavaToKotlinClassMap.getInstance().mapToAnnotationClass(fqName);
        if (mappedClassDescriptor != null) {
            return mappedClassDescriptor;
        }

        final ClassDescriptor annotationClass = classResolver.resolveClass(fqName, INCLUDE_KOTLIN_SOURCES, postponedTasks);
        if (annotationClass == null) {
            return null;
        }

        postponedTasks.addTask(new Runnable() {
            @Override
            public void run() {
                annotation.setAnnotationType(annotationClass.getDefaultType());
            }
        });


        for (JavaAnnotationArgument argument : javaAnnotation.getArguments()) {
            CompileTimeConstant value = argumentResolver.resolveAnnotationArgument(fqName, argument, postponedTasks);
            if (value == null) continue;

            Name name = argument.getName();
            ValueParameterDescriptor descriptor = DescriptorResolverUtils.getAnnotationParameterByName(
                    name == null ? DEFAULT_ANNOTATION_MEMBER_NAME : name, annotationClass);
            if (descriptor != null) {
                annotation.setValueArgument(descriptor, value);
            }
        }

        return annotation;
    }

    @Nullable
    public JavaAnnotation findAnnotationWithExternal(@NotNull JavaAnnotationOwner owner, @NotNull FqName name) {
        JavaAnnotation annotation = owner.findAnnotation(name);
        if (annotation != null) {
            return annotation;
        }

        return externalAnnotationResolver.findExternalAnnotation(owner, name);
    }

    public boolean hasNotNullAnnotation(@NotNull JavaAnnotationOwner owner) {
        return findAnnotationWithExternal(owner, JETBRAINS_NOT_NULL_ANNOTATION) != null;
    }

    public boolean hasMutableAnnotation(@NotNull JavaAnnotationOwner owner) {
        return findAnnotationWithExternal(owner, JETBRAINS_MUTABLE_ANNOTATION) != null;
    }

    public boolean hasReadonlyAnnotation(@NotNull JavaAnnotationOwner owner) {
        return findAnnotationWithExternal(owner, JETBRAINS_READONLY_ANNOTATION) != null;
    }
}