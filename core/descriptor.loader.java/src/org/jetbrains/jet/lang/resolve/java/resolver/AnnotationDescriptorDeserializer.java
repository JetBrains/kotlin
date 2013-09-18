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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;
import org.jetbrains.jet.lang.resolve.constants.ErrorValue;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.vfilefinder.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.asm4.Type.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.kotlinFqNameToJavaFqName;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.naiveKotlinFqName;

public class AnnotationDescriptorDeserializer implements AnnotationDeserializer {
    private static final Logger LOG = Logger.getInstance(AnnotationDescriptorDeserializer.class);

    private JavaClassResolver javaClassResolver;

    private VirtualFileFinder virtualFileFinder;

    // TODO: a single instance of StorageManager for all computations in resolve-java
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();

    private final MemoizedFunctionToNotNull<VirtualFile, Map<MemberSignature, List<AnnotationDescriptor>>> memberAnnotations =
            storageManager.createMemoizedFunction(
                    new MemoizedFunctionToNotNull<VirtualFile, Map<MemberSignature, List<AnnotationDescriptor>>>() {
                        @NotNull
                        @Override
                        public Map<MemberSignature, List<AnnotationDescriptor>> fun(@NotNull VirtualFile file) {
                            try {
                                return loadMemberAnnotationsFromFile(file);
                            }
                            catch (IOException e) {
                                LOG.error("Error loading member annotations from file: " + file, e);
                                return Collections.emptyMap();
                            }
                        }
                    }, StorageManager.ReferenceKind.STRONG);

    @Inject
    public void setVirtualFileFinder(VirtualFileFinder virtualFileFinder) {
        this.virtualFileFinder = virtualFileFinder;
    }

    @Inject
    public void setJavaClassResolver(JavaClassResolver javaClassResolver) {
        this.javaClassResolver = javaClassResolver;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
        VirtualFile virtualFile = findVirtualFileByClass(descriptor);
        if (virtualFile == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: VirtualFileFinder had found the
            // file earlier, but it can't now
            LOG.error("Virtual file for loading class annotations is not found: " + descriptor);
            return Collections.emptyList();
        }
        try {
            return loadClassAnnotationsFromFile(virtualFile);
        }
        catch (IOException e) {
            LOG.error("Error loading member annotations from file: " + virtualFile, e);
            return Collections.emptyList();
        }
    }

    @Nullable
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

    @Nullable
    private VirtualFile findVirtualFileByClass(@NotNull ClassDescriptor descriptor) {
        return virtualFileFinder.find(kotlinFqNameToJavaFqName(naiveKotlinFqName(descriptor)));
    }

    @Nullable
    private VirtualFile findVirtualFileByPackage(@NotNull NamespaceDescriptor descriptor) {
        return virtualFileFinder.find(PackageClassUtils.getPackageClassFqName(DescriptorUtils.getFQName(descriptor).toSafe()));
    }

    @NotNull
    private List<AnnotationDescriptor> loadClassAnnotationsFromFile(@NotNull VirtualFile virtualFile) throws IOException {
        final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

        new ClassReader(virtualFile.contentsToByteArray()).accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return resolveAnnotation(desc, result);
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        return result;
    }

    private static boolean ignoreAnnotation(@NotNull String desc) {
        // TODO: JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION ?
        return desc.equals(JvmAnnotationNames.KOTLIN_CLASS.getDescriptor())
               || desc.equals(JvmAnnotationNames.KOTLIN_PACKAGE.getDescriptor())
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
        final ClassDescriptor annotationClass = resolveAnnotationClass(annotationFqName);
        final AnnotationDescriptor annotation = new AnnotationDescriptor();
        annotation.setAnnotationType(annotationClass.getDefaultType());

        return new AnnotationVisitor(Opcodes.ASM4) {
            // TODO: arrays, annotations, java.lang.Class
            @Override
            public void visit(String name, Object value) {
                CompileTimeConstant<?> argument = JavaAnnotationArgumentResolver.resolveCompileTimeConstantValue(value, null);
                setArgumentValueByName(name, argument != null ? argument : ErrorValue.create("Unsupported annotation argument: " + name));
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                FqName fqName = convertJvmDescriptorToFqName(desc);
                setArgumentValueByName(name, enumEntryValue(fqName, Name.identifier(value)));
            }

            @NotNull
            private CompileTimeConstant<?> enumEntryValue(@NotNull FqName enumFqName, @NotNull Name name) {
                ClassDescriptor enumClass = javaClassResolver.resolveClass(enumFqName, IGNORE_KOTLIN_SOURCES);
                if (enumClass != null && enumClass.getKind() == ClassKind.ENUM_CLASS) {
                    ClassDescriptor classObject = enumClass.getClassObjectDescriptor();
                    if (classObject != null) {
                        Collection<VariableDescriptor> properties = classObject.getDefaultType().getMemberScope().getProperties(name);
                        if (properties.size() == 1) {
                            VariableDescriptor property = properties.iterator().next();
                            if (property instanceof PropertyDescriptor) {
                                return new EnumValue((PropertyDescriptor) property);
                            }
                        }
                    }
                }
                return ErrorValue.create("Unresolved enum entry: " + enumFqName + "." + name);
            }

            @Override
            public void visitEnd() {
                result.add(annotation);
            }

            private void setArgumentValueByName(@NotNull String name, @NotNull CompileTimeConstant<?> argumentValue) {
                ValueParameterDescriptor parameter =
                        DescriptorResolverUtils.getAnnotationParameterByName(Name.identifier(name), annotationClass);
                if (parameter != null) {
                    annotation.setValueArgument(parameter, argumentValue);
                }
            }
        };
    }

    @NotNull
    private ClassDescriptor resolveAnnotationClass(@NotNull FqName fqName) {
        ClassDescriptor annotationClass = javaClassResolver.resolveClass(fqName, IGNORE_KOTLIN_SOURCES);
        return annotationClass != null ? annotationClass : ErrorUtils.getErrorClass();
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

        VirtualFile file = getVirtualFileWithMemberAnnotations(container, proto, nameResolver);
        if (file == null) {
            LOG.error("Virtual file for loading member annotations is not found: " + container);
        }

        List<AnnotationDescriptor> annotations = memberAnnotations.fun(file).get(signature);
        return annotations == null ? Collections.<AnnotationDescriptor>emptyList() : annotations;
    }

    @Nullable
    private VirtualFile getVirtualFileWithMemberAnnotations(
            @NotNull ClassOrNamespaceDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver
    ) {
        if (container instanceof NamespaceDescriptor) {
            Name name = loadSrcClassName(proto, nameResolver);
            if (name != null) {
                // To locate a package$src class, we first find the facade virtual file (*Package.class) and then look up the $src file in
                // the same directory. This hack is needed because FileManager doesn't find classfiles for $src classes
                VirtualFile facadeFile = findVirtualFileByPackage((NamespaceDescriptor) container);
                if (facadeFile != null) {
                    VirtualFile srcFile = facadeFile.getParent().findChild(name + ".class");
                    if (srcFile != null) {
                        return srcFile;
                    }
                }
            }
            return null;
        }
        else if (container instanceof ClassDescriptor && ((ClassDescriptor) container).getKind() == ClassKind.CLASS_OBJECT) {
            // Backing fields of properties of a class object are generated in the outer class
            if (isStaticFieldInOuter(proto)) {
                return findVirtualFileByDescriptor((ClassOrNamespaceDescriptor) container.getContainingDeclaration());
            }
        }

        return findVirtualFileByDescriptor(container);
    }

    @Nullable
    private static Name loadSrcClassName(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        return proto.hasExtension(JavaProtoBuf.srcClassName) ? nameResolver.getName(proto.getExtension(JavaProtoBuf.srcClassName)) : null;
    }

    private static boolean isStaticFieldInOuter(@NotNull ProtoBuf.Callable proto) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return false;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
        return propertySignature.hasField() && propertySignature.getField().getIsStaticInOuter();
    }

    @Nullable
    private static MemberSignature getCallableSignature(
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        switch (kind) {
            case FUNCTION:
                if (proto.hasExtension(JavaProtoBuf.methodSignature)) {
                    JavaProtoBuf.JavaMethodSignature signature = proto.getExtension(JavaProtoBuf.methodSignature);
                    return new SignatureDeserializer(nameResolver).methodSignature(signature);
                }
                break;
            case PROPERTY_GETTER:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
                    return new SignatureDeserializer(nameResolver).methodSignature(propertySignature.getGetter());
                }
                break;
            case PROPERTY_SETTER:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
                    return new SignatureDeserializer(nameResolver).methodSignature(propertySignature.getSetter());
                }
                break;
            case PROPERTY:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);

                    if (propertySignature.hasField()) {
                        JavaProtoBuf.JavaFieldSignature field = propertySignature.getField();
                        Type type = new SignatureDeserializer(nameResolver).type(field.getType());
                        Name name = nameResolver.getName(field.getName());
                        return MemberSignature.fromFieldNameAndDesc(name.asString(), type.getDescriptor());
                    }
                    else if (propertySignature.hasSyntheticMethodName()) {
                        Name name = nameResolver.getName(propertySignature.getSyntheticMethodName());
                        return MemberSignature.fromMethodNameAndDesc(name.asString(), JvmAbi.ANNOTATED_PROPERTY_METHOD_SIGNATURE);
                    }
                }
                break;
        }
        return null;
    }

    @NotNull
    private Map<MemberSignature, List<AnnotationDescriptor>> loadMemberAnnotationsFromFile(@NotNull VirtualFile file) throws IOException {
        final Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations =
                new HashMap<MemberSignature, List<AnnotationDescriptor>>();

        new ClassReader(file.contentsToByteArray()).accept(new ClassVisitor(Opcodes.ASM4) {
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

    private static class SignatureDeserializer {
        // These types are ordered according to their sorts, this is significant for deserialization
        private static final Type[] PRIMITIVE_TYPES = new Type[]
                { VOID_TYPE, BOOLEAN_TYPE, CHAR_TYPE, BYTE_TYPE, SHORT_TYPE, INT_TYPE, FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE };

        private final NameResolver nameResolver;

        public SignatureDeserializer(@NotNull NameResolver nameResolver) {
            this.nameResolver = nameResolver;
        }

        @NotNull
        public MemberSignature methodSignature(@NotNull JavaProtoBuf.JavaMethodSignature signature) {
            String name = nameResolver.getName(signature.getName()).asString();

            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (int i = 0, length = signature.getParameterTypeCount(); i < length; i++) {
                sb.append(type(signature.getParameterType(i)).getDescriptor());
            }
            sb.append(')');
            sb.append(type(signature.getReturnType()).getDescriptor());

            return MemberSignature.fromMethodNameAndDesc(name, sb.toString());
        }

        @NotNull
        public Type type(@NotNull JavaProtoBuf.JavaType type) {
            Type result;
            if (type.hasPrimitiveType()) {
                result = PRIMITIVE_TYPES[type.getPrimitiveType().ordinal()];
            }
            else {
                result = Type.getObjectType(fqNameToInternalName(nameResolver.getFqName(type.getClassFqName())));
            }

            StringBuilder brackets = new StringBuilder(type.getArrayDimension());
            for (int i = 0; i < type.getArrayDimension(); i++) {
                brackets.append('[');
            }

            return Type.getType(brackets + result.getDescriptor());
        }

        @NotNull
        private static String fqNameToInternalName(@NotNull FqName fqName) {
            return fqName.asString().replace('.', '/');
        }
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadValueParameterAnnotations(@NotNull ProtoBuf.Callable.ValueParameter parameterProto) {
        throw new UnsupportedOperationException(); // TODO
    }
}
