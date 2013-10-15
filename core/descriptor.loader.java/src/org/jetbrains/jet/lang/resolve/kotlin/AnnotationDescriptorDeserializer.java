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

import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;
import org.jetbrains.jet.lang.resolve.constants.ErrorValue;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationArgumentResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaClassResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.naiveKotlinFqName;

public class AnnotationDescriptorDeserializer implements AnnotationDeserializer {
    private JavaClassResolver javaClassResolver;
    private KotlinClassFinder kotlinClassFinder;
    private ErrorReporter errorReporter;

    // TODO: a single instance of StorageManager for all computations in resolve-java
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();

    private final MemoizedFunctionToNotNull<KotlinJvmBinaryClass, Map<MemberSignature, List<AnnotationDescriptor>>> memberAnnotations =
            storageManager.createMemoizedFunction(
                    new Function1<KotlinJvmBinaryClass, Map<MemberSignature, List<AnnotationDescriptor>>>() {
                        @NotNull
                        @Override
                        public Map<MemberSignature, List<AnnotationDescriptor>> invoke(@NotNull KotlinJvmBinaryClass kotlinClass) {
                            try {
                                return loadMemberAnnotationsFromClass(kotlinClass);
                            }
                            catch (IOException e) {
                                errorReporter.reportAnnotationLoadingError(
                                        "Error loading member annotations from Kotlin class: " + kotlinClass, e);
                                return Collections.emptyMap();
                            }
                        }
                    });

    @Inject
    public void setJavaClassResolver(JavaClassResolver javaClassResolver) {
        this.javaClassResolver = javaClassResolver;
    }

    @Inject
    public void setKotlinClassFinder(KotlinClassFinder kotlinClassFinder) {
        this.kotlinClassFinder = kotlinClassFinder;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
        KotlinJvmBinaryClass kotlinClass = findKotlinClassByDescriptor(descriptor);
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            errorReporter.reportAnnotationLoadingError("Kotlin class for loading class annotations is not found: " + descriptor, null);
            return Collections.emptyList();
        }
        try {
            return loadClassAnnotationsFromClass(kotlinClass);
        }
        catch (IOException e) {
            errorReporter.reportAnnotationLoadingError("Error loading member annotations from Kotlin class: " + kotlinClass, e);
            return Collections.emptyList();
        }
    }

    @Nullable
    private KotlinJvmBinaryClass findKotlinClassByDescriptor(@NotNull ClassOrNamespaceDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return kotlinClassFinder.find(kotlinFqNameToJavaFqName(naiveKotlinFqName((ClassDescriptor) descriptor)));
        }
        else if (descriptor instanceof NamespaceDescriptor) {
            return kotlinClassFinder.find(PackageClassUtils.getPackageClassFqName(DescriptorUtils.getFQName(descriptor).toSafe()));
        }
        else {
            throw new IllegalStateException("Unrecognized descriptor: " + descriptor);
        }
    }

    @NotNull
    private List<AnnotationDescriptor> loadClassAnnotationsFromClass(@NotNull KotlinJvmBinaryClass kotlinClass) throws IOException {
        final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

        kotlinClass.loadClassAnnotations(new KotlinJvmBinaryClass.AnnotationVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName className) {
                return resolveAnnotation(className, result);
            }

            @Override
            public void visitEnd() {
            }
        });

        return result;
    }

    private static boolean ignoreAnnotation(@NotNull JvmClassName className) {
        // TODO: JETBRAINS_NOT_NULL_ANNOTATION ?
        return className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_CLASS))
               || className.equals(JvmClassName.byFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_PACKAGE))
               || className.getInternalName().startsWith("jet/runtime/typeinfo/");
    }

    @Nullable
    private KotlinJvmBinaryClass.AnnotationArgumentVisitor resolveAnnotation(
            @NotNull JvmClassName className,
            @NotNull final List<AnnotationDescriptor> result
    ) {
        if (ignoreAnnotation(className)) return null;

        final ClassDescriptor annotationClass = resolveClass(className);
        final AnnotationDescriptorImpl annotation = new AnnotationDescriptorImpl();
        annotation.setAnnotationType(annotationClass.getDefaultType());

        return new KotlinJvmBinaryClass.AnnotationArgumentVisitor() {
            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                if (name != null) {
                    CompileTimeConstant<?> argument = JavaAnnotationArgumentResolver.resolveCompileTimeConstantValue(value, null);
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
                ClassDescriptor enumClass = resolveClass(enumClassName);
                if (enumClass.getKind() == ClassKind.ENUM_CLASS) {
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
    private ClassDescriptor resolveClass(@NotNull JvmClassName className) {
        ClassDescriptor annotationClass = javaClassResolver.resolveClass(className.getFqNameForClassNameWithoutDollars(),
                                                                         IGNORE_KOTLIN_SOURCES);
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

        KotlinJvmBinaryClass kotlinClass = findClassWithMemberAnnotations(container, proto, nameResolver);
        if (kotlinClass == null) {
            errorReporter.reportAnnotationLoadingError("Kotlin class for loading member annotations is not found: " + container, null);
            return Collections.emptyList();
        }

        List<AnnotationDescriptor> annotations = memberAnnotations.invoke(kotlinClass).get(signature);
        return annotations == null ? Collections.<AnnotationDescriptor>emptyList() : annotations;
    }

    @Nullable
    private KotlinJvmBinaryClass findClassWithMemberAnnotations(
            @NotNull ClassOrNamespaceDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver
    ) {
        if (container instanceof NamespaceDescriptor) {
            Name name = loadSrcClassName(proto, nameResolver);
            if (name != null) {
                return kotlinClassFinder.find(getSrcClassFqName((NamespaceDescriptor) container, name));
            }
            return null;
        }
        else if (container instanceof ClassDescriptor && ((ClassDescriptor) container).getKind() == ClassKind.CLASS_OBJECT) {
            // Backing fields of properties of a class object are generated in the outer class
            if (isStaticFieldInOuter(proto)) {
                return findKotlinClassByDescriptor((ClassOrNamespaceDescriptor) container.getContainingDeclaration());
            }
        }

        return findKotlinClassByDescriptor(container);
    }

    @NotNull
    private static FqName getSrcClassFqName(@NotNull NamespaceDescriptor container, @NotNull Name name) {
        return PackageClassUtils.getPackageClassFqName(DescriptorUtils.getFQName(container).toSafe()).parent().child(name);
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
                        String type = new SignatureDeserializer(nameResolver).typeDescriptor(field.getType());
                        Name name = nameResolver.getName(field.getName());
                        return MemberSignature.fromFieldNameAndDesc(name, type);
                    }
                    else if (propertySignature.hasSyntheticMethodName()) {
                        Name name = nameResolver.getName(propertySignature.getSyntheticMethodName());
                        return MemberSignature.fromMethodNameAndDesc(name, JvmAbi.ANNOTATED_PROPERTY_METHOD_SIGNATURE);
                    }
                }
                break;
        }
        return null;
    }

    @NotNull
    private Map<MemberSignature, List<AnnotationDescriptor>> loadMemberAnnotationsFromClass(@NotNull KotlinJvmBinaryClass kotlinClass)
            throws IOException {
        final Map<MemberSignature, List<AnnotationDescriptor>> memberAnnotations =
                new HashMap<MemberSignature, List<AnnotationDescriptor>>();

        kotlinClass.loadMemberAnnotations(new KotlinJvmBinaryClass.MemberVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationVisitor visitMethod(@NotNull Name name, @NotNull String desc) {
                return annotationVisitor(MemberSignature.fromMethodNameAndDesc(name, desc));
            }

            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationVisitor visitField(@NotNull Name name, @NotNull String desc) {
                return annotationVisitor(MemberSignature.fromFieldNameAndDesc(name, desc));
            }

            @NotNull
            private KotlinJvmBinaryClass.AnnotationVisitor annotationVisitor(@NotNull final MemberSignature signature) {
                return new KotlinJvmBinaryClass.AnnotationVisitor() {
                    private final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

                    @Nullable
                    @Override
                    public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName className) {
                        return resolveAnnotation(className, result);
                    }

                    @Override
                    public void visitEnd() {
                        if (!result.isEmpty()) {
                            memberAnnotations.put(signature, result);
                        }
                    }
                };
            }
        });

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
        public static MemberSignature fromMethodNameAndDesc(@NotNull Name name, @NotNull String desc) {
            return new MemberSignature(name.asString() + desc);
        }

        @NotNull
        public static MemberSignature fromFieldNameAndDesc(@NotNull Name name, @NotNull String desc) {
            return new MemberSignature(name.asString() + "#" + desc);
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
        private static final char[] PRIMITIVE_TYPES = new char[] { 'V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D' };

        private final NameResolver nameResolver;

        public SignatureDeserializer(@NotNull NameResolver nameResolver) {
            this.nameResolver = nameResolver;
        }

        @NotNull
        public MemberSignature methodSignature(@NotNull JavaProtoBuf.JavaMethodSignature signature) {
            Name name = nameResolver.getName(signature.getName());

            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (int i = 0, length = signature.getParameterTypeCount(); i < length; i++) {
                typeDescriptor(signature.getParameterType(i), sb);
            }
            sb.append(')');
            typeDescriptor(signature.getReturnType(), sb);

            return MemberSignature.fromMethodNameAndDesc(name, sb.toString());
        }

        @NotNull
        public String typeDescriptor(@NotNull JavaProtoBuf.JavaType type) {
            return typeDescriptor(type, new StringBuilder()).toString();
        }

        @NotNull
        private StringBuilder typeDescriptor(@NotNull JavaProtoBuf.JavaType type, @NotNull StringBuilder sb) {
            for (int i = 0; i < type.getArrayDimension(); i++) {
                sb.append('[');
            }

            if (type.hasPrimitiveType()) {
                sb.append(PRIMITIVE_TYPES[type.getPrimitiveType().ordinal()]);
            }
            else {
                sb.append("L");
                sb.append(fqNameToInternalName(nameResolver.getFqName(type.getClassFqName())));
                sb.append(";");
            }

            return sb;
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
