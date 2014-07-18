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
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotatedCallableKind;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationsImpl;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.kotlin.DescriptorLoadersStorage.MemberSignature;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.javaFqNameToKotlinFqName;

public class AnnotationDescriptorLoader extends BaseDescriptorLoader implements AnnotationLoader {

    private ModuleDescriptor module;

    @Inject
    public void setModule(ModuleDescriptor module) {
        this.module = module;
    }

    @Inject
    @Override
    public void setStorage(@NotNull DescriptorLoadersStorage storage) {
        this.storage = storage;
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
            errorReporter.reportLoadingError("Kotlin class for loading class annotations is not found: " + descriptor, null);
            return Annotations.EMPTY;
        }
        try {
            return loadClassAnnotationsFromClass(kotlinClass);
        }
        catch (IOException e) {
            errorReporter.reportLoadingError("Error loading member annotations from Kotlin class: " + kotlinClass, e);
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
                return resolveAnnotation(className, result, module);
            }

            @Override
            public void visitEnd() {
            }
        });

        return new AnnotationsImpl(result);
    }

    @Nullable
    public static KotlinJvmBinaryClass.AnnotationArgumentVisitor resolveAnnotation(
            @NotNull JvmClassName className,
            @NotNull final List<AnnotationDescriptor> result,
            @NotNull final ModuleDescriptor moduleDescriptor
    ) {
        if (JvmAnnotationNames.isSpecialAnnotation(className)) return null;

        final ClassDescriptor annotationClass = resolveClass(className, moduleDescriptor);

        return new KotlinJvmBinaryClass.AnnotationArgumentVisitor() {
            private final Map<ValueParameterDescriptor, CompileTimeConstant<?>> arguments = new HashMap<ValueParameterDescriptor, CompileTimeConstant<?>>();

            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                if (name != null) {
                    setArgumentValueByName(name, createConstant(name, value));
                }
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                setArgumentValueByName(name, enumEntryValue(enumClassName, enumEntryName));
            }

            @Nullable
            @Override
            public AnnotationArrayArgumentVisitor visitArray(@NotNull final Name name) {
                return new KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor() {
                    private final ArrayList<CompileTimeConstant<?>> elements = new ArrayList<CompileTimeConstant<?>>();

                    @Override
                    public void visit(@Nullable Object value) {
                        elements.add(createConstant(name, value));
                    }

                    @Override
                    public void visitEnum(@NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                        elements.add(enumEntryValue(enumClassName, enumEntryName));
                    }

                    @Override
                    public void visitEnd() {
                        ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass);
                        if (parameter != null) {
                            elements.trimToSize();
                            arguments.put(parameter, new ArrayValue(elements, parameter.getType(), true, false));
                        }
                    }
                };
            }

            @NotNull
            private CompileTimeConstant<?> enumEntryValue(@NotNull JvmClassName enumClassName, @NotNull Name name) {
                ClassDescriptor enumClass = resolveClass(enumClassName, moduleDescriptor);
                if (enumClass.getKind() == ClassKind.ENUM_CLASS) {
                    ClassifierDescriptor classifier = enumClass.getUnsubstitutedInnerClassesScope().getClassifier(name);
                    if (classifier instanceof ClassDescriptor) {
                        return new EnumValue((ClassDescriptor) classifier, false);
                    }
                }
                return ErrorValue.create("Unresolved enum entry: " + enumClassName.getInternalName() + "." + name);
            }

            @Override
            public void visitEnd() {
                result.add(new AnnotationDescriptorImpl(
                        annotationClass.getDefaultType(),
                        arguments
                ));
            }

            @NotNull
            private CompileTimeConstant<?> createConstant(@Nullable Name name, @Nullable Object value) {
                CompileTimeConstant<?> argument = ConstantsPackage.createCompileTimeConstant(value, true, false, false, null);
                return argument != null ? argument : ErrorValue.create("Unsupported annotation argument: " + name);
            }

            private void setArgumentValueByName(@NotNull Name name, @NotNull CompileTimeConstant<?> argumentValue) {
                ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass);
                if (parameter != null) {
                    arguments.put(parameter, argumentValue);
                }
            }
        };
    }

    @NotNull
    private static ClassDescriptor resolveClass(@NotNull JvmClassName className, @NotNull ModuleDescriptor moduleDescriptor) {
        FqName packageFqName = className.getPackageFqName();
        FqNameUnsafe relativeClassName = javaFqNameToKotlinFqName(className.getHeuristicClassFqName());
        ClassId classId = new ClassId(packageFqName, relativeClassName);
        ClassDescriptor annotationClass = SerializationPackage.findClassAcrossModuleDependencies(moduleDescriptor, classId);
        return annotationClass != null ? annotationClass : ErrorUtils.createErrorClass(className.getInternalName());
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
        KotlinJvmBinaryClass kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, kind);
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading member annotations is not found: " + container, null);
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
