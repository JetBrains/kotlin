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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.*;

public class AnnotationDescriptorDeserializer implements AnnotationDeserializer {
    private PsiClassFinder psiClassFinder;

    private JavaClassResolver javaClassResolver;

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setJavaClassResolver(JavaClassResolver javaClassResolver) {
        this.javaClassResolver = javaClassResolver;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
        VirtualFile virtualFile = findVirtualFileByClass(descriptor);
        try {
            return loadClassAnnotationsFromFile(virtualFile);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private VirtualFile findVirtualFileByDescriptor(@NotNull ClassOrNamespaceDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return findVirtualFileByClass((ClassDescriptor) descriptor);
        }
        else if (descriptor instanceof NamespaceDescriptor) {
            return findVirtualFileByPackage((NamespaceDescriptor) descriptor);
        }
        else {
            throw new IllegalStateException("Unrecognized descriptor: " + descriptor);
        }
    }

    @NotNull
    private VirtualFile findVirtualFileByClass(@NotNull ClassDescriptor descriptor) {
        FqName fqName = kotlinFqNameToJavaFqName(naiveKotlinFqName(descriptor));
        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE /* TODO: ?! */);
        if (psiClass == null) {
            throw new IllegalStateException("Psi class is not found for class: " + descriptor);
        }
        VirtualFile virtualFile = getVirtualFile(psiClass, fqName, (ClassOrNamespaceDescriptor) descriptor.getContainingDeclaration());
        if (virtualFile == null) {
            throw new IllegalStateException("Virtual file is not found for class: " + descriptor);
        }
        return virtualFile;
    }

    @NotNull
    private VirtualFile findVirtualFileByPackage(@NotNull NamespaceDescriptor descriptor) {
        FqName fqName = PackageClassUtils.getPackageClassFqName(DescriptorUtils.getFQName(descriptor).toSafe());
        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE /* TODO: ?! */);
        if (psiClass == null) {
            throw new IllegalStateException("Psi class is not found for package: " + descriptor);
        }
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            throw new IllegalStateException("Virtual file is not found for package: " + descriptor);
        }
        return virtualFile;
    }

    @NotNull
    private List<AnnotationDescriptor> loadClassAnnotationsFromFile(@NotNull VirtualFile virtualFile) throws IOException {
        final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

        new ClassReader(virtualFile.getInputStream()).accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return resolveAnnotation(desc, result);
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        return result;
    }

    private static boolean ignoreAnnotation(@NotNull String desc) {
        // TODO: JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION ?
        return desc.equals(JvmStdlibNames.KOTLIN_CLASS.getDescriptor())
               || desc.equals(JvmStdlibNames.KOTLIN_PACKAGE.getDescriptor())
               || desc.startsWith("Ljet/runtime/typeinfo/");
    }

    @NotNull
    private static FqName convertJvmDescriptorToFqName(@NotNull String desc) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        String fqName = desc.substring(1, desc.length() - 1).replace('$', '.').replace('/', '.');
        return new FqName(fqName);
    }

    @Nullable
    private AnnotationVisitor resolveAnnotation(@NotNull String desc, @NotNull final List<AnnotationDescriptor> result) {
        if (ignoreAnnotation(desc)) return null;

        FqName annotationFqName = convertJvmDescriptorToFqName(desc);
        final ClassDescriptor annotationClass = javaClassResolver.resolveClass(annotationFqName, ERROR_IF_FOUND_IN_KOTLIN);
        assert annotationClass != null : "Annotation class is not found: " + desc;
        final AnnotationDescriptor annotation = new AnnotationDescriptor();
        annotation.setAnnotationType(annotationClass.getDefaultType());

        return new AnnotationVisitor(Opcodes.ASM4) {
            // TODO: arrays, annotations, enums
            @Override
            public void visit(String name, Object value) {
                ValueParameterDescriptor parameter =
                        DescriptorResolverUtils.getValueParameterDescriptorForAnnotationParameter(Name.identifier(name), annotationClass);
                if (parameter != null) {
                    CompileTimeConstant<?> argument = JavaCompileTimeConstResolver.resolveCompileTimeConstantValue(value, null);
                    if (argument != null) {
                        annotation.setValueArgument(parameter, argument);
                    }
                }
            }

            @Override
            public void visitEnd() {
                result.add(annotation);
            }
        };
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadCallableAnnotations(
            @NotNull ClassOrNamespaceDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        MemberSignature signature = getCallableSignature(proto, nameResolver, kind);
        if (signature == null) return Collections.emptyList();

        VirtualFile file = findVirtualFileByDescriptor(container);

        try {
            // TODO: calculate this only once for each container
            Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations = loadMemberAnnotationsFromFile(file);

            List<AnnotationDescriptor> annotations = memberAnnotations.get(signature);
            return annotations == null ? Collections.<AnnotationDescriptor>emptyList() : annotations;
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @Nullable
    private static MemberSignature getCallableSignature(
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        switch (kind) {
            case FUNCTION:
                return MemberSignature.fromMethod(JavaProtoBufUtil.loadMethodSignature(proto, nameResolver));
            case PROPERTY_GETTER:
                return MemberSignature.fromMethod(JavaProtoBufUtil.loadPropertyGetterSignature(proto, nameResolver));
            case PROPERTY_SETTER:
                return MemberSignature.fromMethod(JavaProtoBufUtil.loadPropertySetterSignature(proto, nameResolver));
            case PROPERTY:
                JavaProtoBufUtil.PropertyData data = JavaProtoBufUtil.loadPropertyData(proto, nameResolver);
                return data == null ? null :
                       MemberSignature.fromPropertyData(data.getFieldType(), data.getFieldName(), data.getSyntheticMethodName());
            default:
                return null;
        }
    }

    @NotNull
    private Map<MemberSignature, List<AnnotationDescriptor>> loadMemberAnnotationsFromFile(@NotNull VirtualFile file) throws IOException {
        final Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations =
                new HashMap<MemberSignature, List<AnnotationDescriptor>>();

        new ClassReader(file.getInputStream()).accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final MemberSignature methodSignature = MemberSignature.fromMethodNameAndDesc(name, desc);
                final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        return resolveAnnotation(desc, result);
                    }

                    @Override
                    public void visitEnd() {
                        if (!result.isEmpty()) {
                            memberAnnotations.put(methodSignature, result);
                        }
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                final MemberSignature fieldSignature = MemberSignature.fromFieldNameAndDesc(name, desc);
                final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

                return new FieldVisitor(Opcodes.ASM4) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        return resolveAnnotation(desc, result);
                    }

                    @Override
                    public void visitEnd() {
                        if (!result.isEmpty()) {
                            memberAnnotations.put(fieldSignature, result);
                        }
                    }
                };
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        return memberAnnotations;
    }

    // The purpose of this class is to hold a unique signature of either a method or a field, so that annotations on a member can be put
    // into a map indexed by these signatures
    private static final class MemberSignature {
        private final String signature;

        private MemberSignature(@NotNull String signature) {
            this.signature = signature;
        }

        @Nullable
        public static MemberSignature fromPropertyData(
                @Nullable Type fieldType,
                @Nullable String fieldName,
                @Nullable String syntheticMethodName
        ) {
            if (fieldName != null && fieldType != null) {
                return fromFieldNameAndDesc(fieldName, fieldType.getDescriptor());
            }
            else if (syntheticMethodName != null) {
                return fromMethodNameAndDesc(syntheticMethodName, JvmAbi.ANNOTATED_PROPERTY_METHOD_SIGNATURE);
            }
            else {
                return null;
            }
        }

        @Nullable
        public static MemberSignature fromMethod(@Nullable Method method) {
            return method == null ? null : fromMethodNameAndDesc(method.getName(), method.getDescriptor());
        }

        @NotNull
        public static MemberSignature fromMethodNameAndDesc(@NotNull String name, @NotNull String desc) {
            return new MemberSignature(name + desc);
        }

        @NotNull
        public static MemberSignature fromFieldNameAndDesc(@NotNull String name, @NotNull String desc) {
            return new MemberSignature(name + "#" + desc);
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

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadValueParameterAnnotations(@NotNull ProtoBuf.Callable.ValueParameter parameterProto) {
        throw new UnsupportedOperationException(); // TODO
    }
}
