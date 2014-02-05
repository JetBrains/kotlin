/*
* Copyright 2010-2014 JetBrains s.r.o.
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

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.ResolverPackage;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.StorageManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

public class DescriptorDeserializersStorage {
    private DependencyClassByQualifiedNameResolver classResolver;
    private ErrorReporter errorReporter;

    private final MemoizedFunctionToNotNull<KotlinJvmBinaryClass, Storage> storage;

    public DescriptorDeserializersStorage(@NotNull StorageManager storageManager) {
        this.storage = storageManager.createMemoizedFunction(
                new Function1<KotlinJvmBinaryClass, Storage>() {
                    @NotNull
                    @Override
                    public Storage invoke(@NotNull KotlinJvmBinaryClass kotlinClass) {
                        try {
                            return loadMemberAnnotationsFromClass(kotlinClass);
                        }
                        catch (IOException e) {
                            errorReporter.reportAnnotationLoadingError(
                                    "Error loading member annotations from Kotlin class: " + kotlinClass, e);
                            return Storage.EMPTY;
                        }
                    }
                });
    }

    @Inject
    public void setClassResolver(DependencyClassByQualifiedNameResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @NotNull
    protected MemoizedFunctionToNotNull<KotlinJvmBinaryClass, Storage> getStorage() {
        return storage;
    }

    @NotNull
    private Storage loadMemberAnnotationsFromClass(@NotNull KotlinJvmBinaryClass kotlinClass) throws IOException {
        final Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations = new HashMap<MemberSignature, List<AnnotationDescriptor>>();
        final Map<MemberSignature, CompileTimeConstant<?>> propertyConstants = new HashMap<MemberSignature, CompileTimeConstant<?>>();

        kotlinClass.loadMemberAnnotations(new KotlinJvmBinaryClass.MemberVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.MethodAnnotationVisitor visitMethod(@NotNull Name name, @NotNull String desc) {
                return new AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name, desc));
            }

            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationVisitor visitField(@NotNull Name name, @NotNull String desc) {
                return new MemberAnnotationVisitor(MemberSignature.fromFieldNameAndDesc(name, desc));
            }

            class AnnotationVisitorForMethod extends MemberAnnotationVisitor implements KotlinJvmBinaryClass.MethodAnnotationVisitor {
                public AnnotationVisitorForMethod(@NotNull MemberSignature signature) {
                    super(signature);
                }

                @Nullable
                @Override
                public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitParameterAnnotation(int index, @NotNull JvmClassName className) {
                    MemberSignature paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index);
                    List<AnnotationDescriptor> result = memberAnnotations.get(paramSignature);
                    if (result == null) {
                        result = new ArrayList<AnnotationDescriptor>();
                        memberAnnotations.put(paramSignature, result);
                    }
                    return AnnotationDescriptorDeserializer.resolveAnnotation(className, result, classResolver);
                }
            }

            class MemberAnnotationVisitor implements KotlinJvmBinaryClass.AnnotationVisitor {
                private final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();
                protected final MemberSignature signature;

                public MemberAnnotationVisitor(@NotNull MemberSignature signature) {
                    this.signature = signature;
                }

                @Nullable
                @Override
                public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName className) {
                    return AnnotationDescriptorDeserializer.resolveAnnotation(className, result, classResolver);
                }

                @Override
                public void visitEnd() {
                    if (!result.isEmpty()) {
                        memberAnnotations.put(signature, result);
                    }
                }
            }
        });

        return new Storage(memberAnnotations, propertyConstants);
    }

    // The purpose of this class is to hold a unique signature of either a method or a field, so that annotations on a member can be put
    // into a map indexed by these signatures
    protected static final class MemberSignature {
        private final String signature;

        private MemberSignature(@NotNull String signature) {
            this.signature = signature;
        }

        @NotNull
        public static MemberSignature fromMethodNameAndDesc(@NotNull Name name, @NotNull String desc) {
            return new MemberSignature(name.asString() + desc);
        }

        @NotNull
        public static MemberSignature fromFieldNameAndDesc(@NotNull Name name, @NotNull String desc) {
            return new MemberSignature(name.asString() + "#" + desc);
        }

        @NotNull
        public static MemberSignature fromMethodSignatureAndParameterIndex(@NotNull MemberSignature signature, int index) {
            return new MemberSignature(signature.signature + "@" + index);
        }

        @Override
        public int hashCode() {
            return signature.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MemberSignature && signature.equals(((MemberSignature) o).signature);
        }

        @Override
        public String toString() {
            return signature;
        }
    }

    protected static class Storage {
        private final Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations;
        private final Map<MemberSignature, CompileTimeConstant<?>> propertyConstants;

        public static final Storage EMPTY = new Storage(
                Collections.<MemberSignature, List<AnnotationDescriptor>>emptyMap(),
                Collections.<MemberSignature, CompileTimeConstant<?>>emptyMap()
        );

        public Storage(
                @NotNull Map<MemberSignature, List<AnnotationDescriptor>> annotations,
                @NotNull Map<MemberSignature, CompileTimeConstant<?>> constants
        ) {
            this.memberAnnotations = annotations;
            this.propertyConstants = constants;
        }

        public Map<MemberSignature, List<AnnotationDescriptor>> getMemberAnnotations() {
            return memberAnnotations;
        }

        public Map<MemberSignature, CompileTimeConstant<?>> getPropertyConstants() {
            return propertyConstants;
        }
    }
}
