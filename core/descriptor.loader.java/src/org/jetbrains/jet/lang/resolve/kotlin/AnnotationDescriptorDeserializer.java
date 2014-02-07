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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationsImpl;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.ConstantUtils;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;
import org.jetbrains.jet.lang.resolve.constants.ErrorValue;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.ResolverPackage;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;
import static org.jetbrains.jet.lang.resolve.kotlin.DescriptorDeserializersStorage.MemberSignature;

public class AnnotationDescriptorDeserializer extends BaseDescriptorDeserializer implements AnnotationDeserializer {
    @Inject
    @Override
    public void setStorage(@NotNull DescriptorDeserializersStorage storage) {
        this.storage = storage;
    }

    @Inject
    @Override
    public void setClassResolver(@NotNull DependencyClassByQualifiedNameResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    @Override
    public void setKotlinClassFinder(@NotNull KotlinClassFinder kotlinClassFinder) {
        this.kotlinClassFinder = kotlinClassFinder;
    }

    @Inject
    @Override
    public void setErrorReporter(@NotNull ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @NotNull
    @Override
    public Annotations loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
        KotlinJvmBinaryClass kotlinClass = findKotlinClassByDescriptor(descriptor);
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            errorReporter.reportAnnotationLoadingError("Kotlin class for loading class annotations is not found: " + descriptor, null);
            return Annotations.EMPTY;
        }
        try {
            return loadClassAnnotationsFromClass(kotlinClass);
        }
        catch (IOException e) {
            errorReporter.reportAnnotationLoadingError("Error loading member annotations from Kotlin class: " + kotlinClass, e);
            return Annotations.EMPTY;
        }
    }

    @NotNull
    private Annotations loadClassAnnotationsFromClass(@NotNull KotlinJvmBinaryClass kotlinClass) throws IOException {
        final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

        kotlinClass.loadClassAnnotations(new KotlinJvmBinaryClass.AnnotationVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName className) {
                return resolveAnnotation(className, result, classResolver);
            }

            @Override
            public void visitEnd() {
            }
        });

        return new AnnotationsImpl(result);
    }

    private static boolean ignoreAnnotation(@NotNull JvmClassName className) {
        return className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_CLASS))
               || className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_PACKAGE))
               || className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION))
               || className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION))
               || className.getInternalName().startsWith("jet/runtime/typeinfo/");
    }

    @Nullable
    public static KotlinJvmBinaryClass.AnnotationArgumentVisitor resolveAnnotation(
            @NotNull JvmClassName className,
            @NotNull final List<AnnotationDescriptor> result,
            @NotNull final DependencyClassByQualifiedNameResolver classResolver
    ) {
        if (ignoreAnnotation(className)) return null;

        final ClassDescriptor annotationClass = resolveClass(className, classResolver);
        final AnnotationDescriptorImpl annotation = new AnnotationDescriptorImpl();
        annotation.setAnnotationType(annotationClass.getDefaultType());

        return new KotlinJvmBinaryClass.AnnotationArgumentVisitor() {
            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                if (name != null) {
                    CompileTimeConstant<?> argument = ConstantUtils.createCompileTimeConstant(value, true, false, null);
                    setArgumentValueByName(name, argument != null ? argument : ErrorValue.create("Unsupported annotation argument: " + name));
                }
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                setArgumentValueByName(name, enumEntryValue(enumClassName, enumEntryName));
            }

            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitArray(@NotNull Name name) {
                // TODO: support arrays
                return null;
            }

            @NotNull
            private CompileTimeConstant<?> enumEntryValue(@NotNull JvmClassName enumClassName, @NotNull Name name) {
                ClassDescriptor enumClass = resolveClass(enumClassName, classResolver);
                if (enumClass.getKind() == ClassKind.ENUM_CLASS) {
                    ClassifierDescriptor classifier = enumClass.getUnsubstitutedInnerClassesScope().getClassifier(name);
                    if (classifier instanceof ClassDescriptor) {
                        return new EnumValue((ClassDescriptor) classifier);
                    }
                }
                return ErrorValue.create("Unresolved enum entry: " + enumClassName.getInternalName() + "." + name);
            }

            @Override
            public void visitEnd() {
                result.add(annotation);
            }

            private void setArgumentValueByName(@NotNull Name name, @NotNull CompileTimeConstant<?> argumentValue) {
                ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass);
                if (parameter != null) {
                    annotation.setValueArgument(parameter, argumentValue);
                }
            }
        };
    }

    @NotNull
    private static ClassDescriptor resolveClass(@NotNull JvmClassName className, DependencyClassByQualifiedNameResolver classResolver) {
        ClassDescriptor annotationClass = classResolver.resolveClass(className.getFqNameForClassNameWithoutDollars());
        return annotationClass != null ? annotationClass : ErrorUtils.getErrorClass();
    }

    @NotNull
    @Override
    public Annotations loadCallableAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        MemberSignature signature = getCallableSignature(proto, nameResolver, kind);
        if (signature == null) return Annotations.EMPTY;

        return findClassAndLoadMemberAnnotations(container, proto, nameResolver, kind, signature);
    }

    @NotNull
    private Annotations findClassAndLoadMemberAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull MemberSignature signature
    ) {
        KotlinJvmBinaryClass kotlinClass = findClassWithMemberAnnotations(container, proto, nameResolver, kind);
        if (kotlinClass == null) {
            errorReporter.reportAnnotationLoadingError("Kotlin class for loading member annotations is not found: " + container, null);
            return Annotations.EMPTY;
        }

        List<AnnotationDescriptor> annotations = storage.getStorage().invoke(kotlinClass).getMemberAnnotations().get(signature);
        return annotations == null ? Annotations.EMPTY : new AnnotationsImpl(annotations);
    }

    @NotNull
    @Override
    public Annotations loadValueParameterAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    ) {
        MemberSignature methodSignature = getCallableSignature(callable, nameResolver, kind);
        if (methodSignature != null) {
            if (proto.hasExtension(JavaProtoBuf.index)) {
                MemberSignature paramSignature =
                        MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, proto.getExtension(JavaProtoBuf.index));
                return findClassAndLoadMemberAnnotations(container, callable, nameResolver, kind, paramSignature);
            }
        }

        return Annotations.EMPTY;
    }
}
