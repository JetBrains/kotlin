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

package org.jetbrains.jet.descriptors.serialization;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;

public class DescriptorSerializer {

    private static final DescriptorRenderer RENDERER = DescriptorRenderer.STARTS_FROM_NAME;
    private static final Comparator<DeclarationDescriptor> DESCRIPTOR_COMPARATOR = new Comparator<DeclarationDescriptor>() {
        @Override
        public int compare(@NotNull DeclarationDescriptor o1, @NotNull DeclarationDescriptor o2) {
            int names = o1.getName().compareTo(o2.getName());
            if (names != 0) return names;

            String o1String = RENDERER.render(o1);
            String o2String = RENDERER.render(o2);
            return o1String.compareTo(o2String);
        }
    };
    private final NameTable nameTable;
    private final Interner<TypeParameterDescriptor> typeParameters;
    private final SerializerExtension extension;

    public DescriptorSerializer() {
        this(SerializerExtension.DEFAULT);
    }

    public DescriptorSerializer(@NotNull SerializerExtension extension) {
        this(new NameTable(), new Interner<TypeParameterDescriptor>(), extension);
    }

    private DescriptorSerializer(NameTable nameTable, Interner<TypeParameterDescriptor> typeParameters, SerializerExtension extension) {
        this.nameTable = nameTable;
        this.typeParameters = typeParameters;
        this.extension = extension;
    }

    private DescriptorSerializer createChildSerializer() {
        return new DescriptorSerializer(nameTable, new Interner<TypeParameterDescriptor>(typeParameters), extension);
    }

    @NotNull
    public NameTable getNameTable() {
        return nameTable;
    }

    @NotNull
    public ProtoBuf.Class.Builder classProto(@NotNull ClassDescriptor classDescriptor) {
        ProtoBuf.Class.Builder builder = ProtoBuf.Class.newBuilder();

        int flags = Flags.getClassFlags(hasAnnotations(classDescriptor), classDescriptor.getVisibility(),
                                        classDescriptor.getModality(), classDescriptor.getKind(), classDescriptor.isInner());
        builder.setFlags(flags);

        // TODO extra visibility

        builder.setFqName(getClassId(classDescriptor));

        DescriptorSerializer local = createChildSerializer();

        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor));
        }

        if (extension.hasSupertypes(classDescriptor)) {
            // Special classes (Any, Nothing) have no supertypes
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                builder.addSupertype(local.type(supertype));
            }
        }

        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor != null) {
            if (DescriptorFactory.isDefaultPrimaryConstructor(primaryConstructor)) {
                builder.setPrimaryConstructor(ProtoBuf.Class.PrimaryConstructor.getDefaultInstance());
            }
            else {
                ProtoBuf.Class.PrimaryConstructor.Builder constructorBuilder = ProtoBuf.Class.PrimaryConstructor.newBuilder();
                constructorBuilder.setData(local.callableProto(primaryConstructor));
                builder.setPrimaryConstructor(constructorBuilder);
            }
        }

        // TODO: other constructors

        for (DeclarationDescriptor descriptor : sort(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors())) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor member = (CallableMemberDescriptor) descriptor;
                if (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue;
                builder.addMember(local.callableProto(member));
            }
        }

        Collection<DeclarationDescriptor> nestedClasses = classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors();
        for (DeclarationDescriptor descriptor : sort(nestedClasses)) {
            if (!isEnumEntry(descriptor)) {
                builder.addNestedClassName(nameTable.getSimpleNameIndex(descriptor.getName()));
            }
        }

        ClassDescriptor classObject = classDescriptor.getClassObjectDescriptor();
        if (classObject != null) {
            builder.setClassObject(classObjectProto(classObject));
        }

        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
            // Not calling sort() here, because the order of enum entries matters
            for (DeclarationDescriptor descriptor : nestedClasses) {
                if (isEnumEntry(descriptor)) {
                    builder.addEnumEntry(nameTable.getSimpleNameIndex(descriptor.getName()));
                }
            }
        }

        return builder;
    }

    @NotNull
    private ProtoBuf.Class.ClassObject classObjectProto(@NotNull ClassDescriptor classObject) {
        if (isObject(classObject.getContainingDeclaration())) {
            return ProtoBuf.Class.ClassObject.newBuilder().setData(classProto(classObject)).build();
        }

        return ProtoBuf.Class.ClassObject.getDefaultInstance();
    }

    @NotNull
    public ProtoBuf.Callable.Builder callableProto(@NotNull CallableMemberDescriptor descriptor) {
        ProtoBuf.Callable.Builder builder = ProtoBuf.Callable.newBuilder();

        DescriptorSerializer local = createChildSerializer();

        boolean hasGetter = false;
        boolean hasSetter = false;
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

            int propertyFlags = Flags.getAccessorFlags(
                    hasAnnotations(propertyDescriptor),
                    propertyDescriptor.getVisibility(),
                    propertyDescriptor.getModality(),
                    false
            );

            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            if (getter != null) {
                hasGetter = true;
                int accessorFlags = getAccessorFlags(getter);
                if (accessorFlags != propertyFlags) {
                    builder.setGetterFlags(accessorFlags);
                }
            }

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (setter != null) {
                hasSetter = true;
                int accessorFlags = getAccessorFlags(setter);
                if (accessorFlags != propertyFlags) {
                    builder.setSetterFlags(accessorFlags);
                }

                if (!setter.isDefault()) {
                    for (ValueParameterDescriptor valueParameterDescriptor : setter.getValueParameters()) {
                        builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
                    }
                }
            }
        }

        builder.setFlags(Flags.getCallableFlags(
                hasAnnotations(descriptor),
                descriptor.getVisibility(),
                descriptor.getModality(),
                descriptor.getKind(),
                callableKind(descriptor),
                hasGetter,
                hasSetter
        ));
        //TODO builder.setExtraVisibility()

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor));
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            builder.setReceiverType(local.type(receiverParameter.getType()));
        }

        builder.setName(nameTable.getSimpleNameIndex(descriptor.getName()));

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
        }

        builder.setReturnType(local.type(getSerializableReturnType(descriptor.getReturnType())));

        extension.serializeCallable(descriptor, builder, nameTable);

        return builder;
    }

    @NotNull
    private static JetType getSerializableReturnType(@NotNull JetType type) {
        return isSerializableType(type) ? type : KotlinBuiltIns.getInstance().getAnyType();
    }

    /**
     * @return true iff this type can be serialized. Types which correspond to type parameters, top-level classes, inner classes, and
     * generic classes with serializable arguments are serializable. For other types (local classes, inner of local, etc.) it may be
     * problematical to construct a FQ name for serialization
     */
    private static boolean isSerializableType(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor instanceof TypeParameterDescriptor) {
            return true;
        }
        else if (descriptor instanceof ClassDescriptor) {
            for (TypeProjection projection : type.getArguments()) {
                if (!isSerializableType(projection.getType())) {
                    return false;
                }
            }

            return isTopLevelOrInnerClass((ClassDescriptor) descriptor);
        }
        else {
            throw new IllegalStateException("Unknown type constructor: " + type);
        }
    }

    private static int getAccessorFlags(@NotNull PropertyAccessorDescriptor accessor) {
        return Flags.getAccessorFlags(
                hasAnnotations(accessor),
                accessor.getVisibility(),
                accessor.getModality(),
                !accessor.isDefault()
        );
    }

    @NotNull
    private static ProtoBuf.Callable.CallableKind callableKind(@NotNull CallableMemberDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return ((PropertyDescriptor) descriptor).isVar() ? ProtoBuf.Callable.CallableKind.VAR : ProtoBuf.Callable.CallableKind.VAL;
        }
        if (descriptor instanceof ConstructorDescriptor) {
            return ProtoBuf.Callable.CallableKind.CONSTRUCTOR;
        }
        assert descriptor instanceof FunctionDescriptor : "Unknown descriptor class: " + descriptor.getClass();
        return ProtoBuf.Callable.CallableKind.FUN;
    }

    @NotNull
    private ProtoBuf.Callable.ValueParameter.Builder valueParameter(@NotNull ValueParameterDescriptor descriptor) {
        ProtoBuf.Callable.ValueParameter.Builder builder = ProtoBuf.Callable.ValueParameter.newBuilder();

        builder.setFlags(Flags.getValueParameterFlags(hasAnnotations(descriptor), descriptor.declaresDefaultValue()));

        builder.setName(nameTable.getSimpleNameIndex(descriptor.getName()));

        builder.setType(type(descriptor.getType()));

        JetType varargElementType = descriptor.getVarargElementType();
        if (varargElementType != null) {
            builder.setVarargElementType(type(varargElementType));
        }

        extension.serializeValueParameter(descriptor, builder, nameTable);

        return builder;
    }

    private ProtoBuf.TypeParameter.Builder typeParameter(TypeParameterDescriptor typeParameter) {
        ProtoBuf.TypeParameter.Builder builder = ProtoBuf.TypeParameter.newBuilder();

        builder.setId(getTypeParameterId(typeParameter));

        builder.setName(nameTable.getSimpleNameIndex(typeParameter.getName()));

        // to avoid storing a default
        if (typeParameter.isReified()) {
            builder.setReified(true);
        }

        // to avoid storing a default
        ProtoBuf.TypeParameter.Variance variance = variance(typeParameter.getVariance());
        if (variance != ProtoBuf.TypeParameter.Variance.INV) {
            builder.setVariance(variance);
        }

        for (JetType upperBound : typeParameter.getUpperBounds()) {
            builder.addUpperBound(type(upperBound));
        }

        return builder;
    }

    private static ProtoBuf.TypeParameter.Variance variance(Variance variance) {
        switch (variance) {
            case INVARIANT:
                return ProtoBuf.TypeParameter.Variance.INV;
            case IN_VARIANCE:
                return ProtoBuf.TypeParameter.Variance.IN;
            case OUT_VARIANCE:
                return  ProtoBuf.TypeParameter.Variance.OUT;
        }
        throw new IllegalStateException("Unknown variance: " + variance);
    }

    @NotNull
    public ProtoBuf.Type.Builder type(@NotNull JetType type) {
        assert !type.isError() : "Can't serialize error types: " + type; // TODO

        ProtoBuf.Type.Builder builder = ProtoBuf.Type.newBuilder();

        builder.setConstructor(typeConstructor(type.getConstructor()));

        for (TypeProjection projection : type.getArguments()) {
            builder.addArgument(typeArgument(projection));
        }

        // to avoid storing a default
        if (type.isNullable()) {
            builder.setNullable(true);
        }

        return builder;
    }

    @NotNull
    private ProtoBuf.Type.Argument.Builder typeArgument(@NotNull TypeProjection typeProjection) {
        ProtoBuf.Type.Argument.Builder builder = ProtoBuf.Type.Argument.newBuilder();
        ProtoBuf.Type.Argument.Projection projection = projection(typeProjection.getProjectionKind());

        // to avoid storing a default
        if (projection != ProtoBuf.Type.Argument.Projection.INV) {
            builder.setProjection(projection);
        }

        builder.setType(type(typeProjection.getType()));
        return builder;
    }

    @NotNull
    private ProtoBuf.Type.Constructor.Builder typeConstructor(@NotNull TypeConstructor typeConstructor) {
        ProtoBuf.Type.Constructor.Builder builder = ProtoBuf.Type.Constructor.newBuilder();

        ClassifierDescriptor declarationDescriptor = typeConstructor.getDeclarationDescriptor();

        assert declarationDescriptor instanceof TypeParameterDescriptor || declarationDescriptor instanceof ClassDescriptor
                : "Unknown declaration descriptor: " + typeConstructor;
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;
            builder.setKind(ProtoBuf.Type.Constructor.Kind.TYPE_PARAMETER);
            builder.setId(getTypeParameterId(typeParameterDescriptor));
        }
        else {
            ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
            //default: builder.setKind(Type.Constructor.Kind.CLASS);
            builder.setId(getClassId(classDescriptor));
        }
        return builder;
    }

    @NotNull
    public ProtoBuf.Package.Builder packageProto(@NotNull Collection<PackageFragmentDescriptor> fragments) {
        ProtoBuf.Package.Builder builder = ProtoBuf.Package.newBuilder();

        Collection<DeclarationDescriptor> members = Lists.newArrayList();
        for (PackageFragmentDescriptor fragment : fragments) {
            members.addAll(fragment.getMemberScope().getAllDescriptors());
        }

        for (DeclarationDescriptor declaration : sort(members)) {
            if (declaration instanceof PropertyDescriptor || declaration instanceof FunctionDescriptor) {
                builder.addMember(callableProto((CallableMemberDescriptor) declaration));
            }
        }

        return builder;
    }

    @NotNull
    private static ProtoBuf.Type.Argument.Projection projection(@NotNull Variance projectionKind) {
        switch (projectionKind) {
            case INVARIANT:
                return ProtoBuf.Type.Argument.Projection.INV;
            case IN_VARIANCE:
                return ProtoBuf.Type.Argument.Projection.IN;
            case OUT_VARIANCE:
                return ProtoBuf.Type.Argument.Projection.OUT;
        }
        throw new IllegalStateException("Unknown projectionKind: " + projectionKind);
    }

    private int getClassId(@NotNull ClassDescriptor descriptor) {
        return nameTable.getFqNameIndex(descriptor);
    }

    private int getTypeParameterId(@NotNull TypeParameterDescriptor descriptor) {
        return typeParameters.intern(descriptor);
    }

    private static boolean hasAnnotations(Annotated descriptor) {
        return !descriptor.getAnnotations().isEmpty();
    }

    @NotNull
    public static <T extends DeclarationDescriptor> List<T> sort(@NotNull Collection<T> descriptors) {
        List<T> result = new ArrayList<T>(descriptors);
        Collections.sort(result, DESCRIPTOR_COMPARATOR);
        return result;

    }
}
