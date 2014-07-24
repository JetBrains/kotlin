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
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotatedCallableKind;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;
import static org.jetbrains.jet.lang.resolve.kotlin.DescriptorLoadersStorage.MemberSignature;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName;
import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.naiveKotlinFqName;

public abstract class BaseDescriptorLoader {
    protected KotlinClassFinder kotlinClassFinder;
    protected ErrorReporter errorReporter;

    protected DescriptorLoadersStorage storage;

    public abstract void setKotlinClassFinder(@NotNull KotlinClassFinder kotlinClassFinder);

    public abstract void setErrorReporter(@NotNull ErrorReporter errorReporter);

    public abstract void setStorage(@NotNull DescriptorLoadersStorage storage);

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
    protected KotlinJvmBinaryClass findClassWithAnnotationsAndInitializers(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        if (container instanceof PackageFragmentDescriptor) {
            return getPackagePartClassFqNameSafe((PackageFragmentDescriptor) container, proto, nameResolver);
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
    private KotlinJvmBinaryClass getPackagePartClassFqNameSafe(
            @NotNull PackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver
    ) {
        if (proto.hasExtension(JavaProtoBuf.implClassName)) {
            return kotlinClassFinder.findKotlinClass(container.getFqName().child(getPackagePartClassName(proto, nameResolver)));
        }
        return null;
    }

    @NotNull
    public static Name getPackagePartClassName(@NotNull DeserializedCallableMemberDescriptor deserializedCallableMember) {
        return getPackagePartClassName(deserializedCallableMember.getProto(), deserializedCallableMember.getNameResolver());
    }

    @NotNull
    private static Name getPackagePartClassName(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        return nameResolver.getName(proto.getExtension(JavaProtoBuf.implClassName));
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
}
