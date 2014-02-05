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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;

import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;
import static org.jetbrains.jet.lang.resolve.kotlin.DescriptorDeserializersStorage.MemberSignature;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.naiveKotlinFqName;

public abstract class BaseDescriptorDeserializer {
    protected DependencyClassByQualifiedNameResolver classResolver;
    protected KotlinClassFinder kotlinClassFinder;
    protected ErrorReporter errorReporter;

    protected DescriptorDeserializersStorage storage;

    public abstract void setClassResolver(@NotNull DependencyClassByQualifiedNameResolver classResolver);

    public abstract void setKotlinClassFinder(@NotNull KotlinClassFinder kotlinClassFinder);

    public abstract void setErrorReporter(@NotNull ErrorReporter errorReporter);

    public abstract void setStorage(@NotNull DescriptorDeserializersStorage storage);

    @Nullable
    protected static MemberSignature getCallableSignature(
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        SignatureDeserializer deserializer = new SignatureDeserializer(nameResolver);
        switch (kind) {
            case FUNCTION:
                if (proto.hasExtension(JavaProtoBuf.methodSignature)) {
                    return deserializer.methodSignature(proto.getExtension(JavaProtoBuf.methodSignature));
                }
                break;
            case PROPERTY_GETTER:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    return deserializer.methodSignature(proto.getExtension(JavaProtoBuf.propertySignature).getGetter());
                }
                break;
            case PROPERTY_SETTER:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    return deserializer.methodSignature(proto.getExtension(JavaProtoBuf.propertySignature).getSetter());
                }
                break;
            case PROPERTY:
                if (proto.hasExtension(JavaProtoBuf.propertySignature)) {
                    JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);

                    if (propertySignature.hasField()) {
                        JavaProtoBuf.JavaFieldSignature field = propertySignature.getField();
                        String type = deserializer.typeDescriptor(field.getType());
                        Name name = nameResolver.getName(field.getName());
                        return MemberSignature.fromFieldNameAndDesc(name, type);
                    }
                    else if (propertySignature.hasSyntheticMethod()) {
                        return deserializer.methodSignature(propertySignature.getSyntheticMethod());
                    }
                }
                break;
        }
        return null;
    }

    @Nullable
    protected KotlinJvmBinaryClass findClassWithMemberAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        if (container instanceof PackageFragmentDescriptor) {
            return loadPackageFragmentClassFqName((PackageFragmentDescriptor) container, proto, nameResolver);
        }
        else if (isClassObject(container) && isStaticFieldInOuter(proto)) {
            // Backing fields of properties of a class object are generated in the outer class
            return findKotlinClassByDescriptor((ClassOrPackageFragmentDescriptor) container.getContainingDeclaration());
        }
        else if (isTrait(container) && kind == AnnotatedCallableKind.PROPERTY) {
            PackageFragmentDescriptor containingPackage = DescriptorUtils.getParentOfType(container, PackageFragmentDescriptor.class);
            assert containingPackage != null : "Trait must have a package fragment among his parents: " + container;

            if (proto.hasExtension(JavaProtoBuf.implClassName)) {
                Name tImplName = nameResolver.getName(proto.getExtension(JavaProtoBuf.implClassName));
                return kotlinClassFinder.findKotlinClass(containingPackage.getFqName().child(tImplName));
            }
            return null;
        }

        return findKotlinClassByDescriptor(container);
    }

    @Nullable
    private KotlinJvmBinaryClass loadPackageFragmentClassFqName(
            @NotNull PackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver
    ) {
        if (proto.hasExtension(JavaProtoBuf.implClassName)) {
            Name name = nameResolver.getName(proto.getExtension(JavaProtoBuf.implClassName));
            FqName fqName = PackageClassUtils.getPackageClassFqName(container.getFqName()).parent().child(name);
            return kotlinClassFinder.findKotlinClass(fqName);
        }
        return null;
    }

    private static boolean isStaticFieldInOuter(@NotNull ProtoBuf.Callable proto) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return false;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
        return propertySignature.hasField() && propertySignature.getField().getIsStaticInOuter();
    }

    @Nullable
    protected KotlinJvmBinaryClass findKotlinClassByDescriptor(@NotNull ClassOrPackageFragmentDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return kotlinClassFinder.findKotlinClass(kotlinFqNameToJavaFqName(naiveKotlinFqName((ClassDescriptor) descriptor)));
        }
        else if (descriptor instanceof PackageFragmentDescriptor) {
            return kotlinClassFinder.findKotlinClass(
                    PackageClassUtils.getPackageClassFqName(((PackageFragmentDescriptor) descriptor).getFqName()));
        }
        else {
            throw new IllegalStateException("Unrecognized descriptor: " + descriptor);
        }
    }

    protected static class SignatureDeserializer {
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
}
