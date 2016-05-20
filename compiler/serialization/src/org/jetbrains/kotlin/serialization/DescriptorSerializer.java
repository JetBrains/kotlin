/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization;

import com.google.protobuf.MessageLite;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.MemberComparator;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.Interner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;

public class DescriptorSerializer {
    private final DeclarationDescriptor containingDeclaration;
    private final Interner<TypeParameterDescriptor> typeParameters;
    private final SerializerExtension extension;
    private final MutableTypeTable typeTable;
    private final boolean serializeTypeTableToFunction;

    private DescriptorSerializer(
            @Nullable DeclarationDescriptor containingDeclaration,
            @NotNull Interner<TypeParameterDescriptor> typeParameters,
            @NotNull SerializerExtension extension,
            @NotNull MutableTypeTable typeTable,
            boolean serializeTypeTableToFunction
    ) {
        this.containingDeclaration = containingDeclaration;
        this.typeParameters = typeParameters;
        this.extension = extension;
        this.typeTable = typeTable;
        this.serializeTypeTableToFunction = serializeTypeTableToFunction;
    }

    @NotNull
    public byte[] serialize(@NotNull MessageLite message) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            getStringTable().serializeTo(result);
            message.writeTo(result);
            return result.toByteArray();
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    public static DescriptorSerializer createTopLevel(@NotNull SerializerExtension extension) {
        return new DescriptorSerializer(null, new Interner<TypeParameterDescriptor>(), extension, new MutableTypeTable(), false);
    }

    @NotNull
    public static DescriptorSerializer createForLambda(@NotNull SerializerExtension extension) {
        return new DescriptorSerializer(null, new Interner<TypeParameterDescriptor>(), extension, new MutableTypeTable(), true);
    }

    @NotNull
    public static DescriptorSerializer create(@NotNull ClassDescriptor descriptor, @NotNull SerializerExtension extension) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        DescriptorSerializer parentSerializer =
                container instanceof ClassDescriptor
                ? create((ClassDescriptor) container, extension)
                : createTopLevel(extension);

        // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
        // serializing outer classes before nested classes.
        // Otherwise our interner can get wrong ids because we may serialize classes in any order.
        DescriptorSerializer serializer = new DescriptorSerializer(
                descriptor,
                new Interner<TypeParameterDescriptor>(parentSerializer.typeParameters),
                parentSerializer.extension,
                new MutableTypeTable(),
                false
        );
        for (TypeParameterDescriptor typeParameter : descriptor.getDeclaredTypeParameters()) {
            serializer.typeParameters.intern(typeParameter);
        }
        return serializer;
    }

    @NotNull
    private DescriptorSerializer createChildSerializer(@NotNull CallableDescriptor callable) {
        return new DescriptorSerializer(callable, new Interner<TypeParameterDescriptor>(typeParameters), extension, typeTable, false);
    }

    @NotNull
    public StringTable getStringTable() {
        return extension.getStringTable();
    }

    private boolean useTypeTable() {
        return extension.shouldUseTypeTable();
    }

    @NotNull
    public ProtoBuf.Class.Builder classProto(@NotNull ClassDescriptor classDescriptor) {
        ProtoBuf.Class.Builder builder = ProtoBuf.Class.newBuilder();

        int flags = Flags.getClassFlags(hasAnnotations(classDescriptor), classDescriptor.getVisibility(), classDescriptor.getModality(),
                                        classDescriptor.getKind(), classDescriptor.isInner(), classDescriptor.isCompanionObject(),
                                        classDescriptor.isData());
        if (flags != builder.getFlags()) {
            builder.setFlags(flags);
        }

        builder.setFqName(getClassId(classDescriptor));

        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getDeclaredTypeParameters()) {
            builder.addTypeParameter(typeParameter(typeParameterDescriptor));
        }

        if (!KotlinBuiltIns.isSpecialClassWithNoSupertypes(classDescriptor)) {
            // Special classes (Any, Nothing) have no supertypes
            for (KotlinType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                if (useTypeTable()) {
                    builder.addSupertypeId(typeId(supertype));
                }
                else {
                    builder.addSupertype(type(supertype));
                }
            }
        }

        for (ConstructorDescriptor descriptor : classDescriptor.getConstructors()) {
            builder.addConstructor(constructorProto(descriptor));
        }

        for (DeclarationDescriptor descriptor : sort(DescriptorUtils.getAllDescriptors(classDescriptor.getDefaultType().getMemberScope()))) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor member = (CallableMemberDescriptor) descriptor;
                if (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue;

                if (descriptor instanceof PropertyDescriptor) {
                    builder.addProperty(propertyProto((PropertyDescriptor) descriptor));
                }
                else if (descriptor instanceof FunctionDescriptor) {
                    builder.addFunction(functionProto((FunctionDescriptor) descriptor));
                }
            }
        }

        for (DeclarationDescriptor descriptor : sort(DescriptorUtils.getAllDescriptors(classDescriptor.getUnsubstitutedInnerClassesScope()))) {
            int name = getSimpleNameIndex(descriptor.getName());
            if (isEnumEntry(descriptor)) {
                builder.addEnumEntry(enumEntryProto((ClassDescriptor) descriptor));
            }
            else {
                builder.addNestedClassName(name);
            }
        }

        ClassDescriptor companionObjectDescriptor = classDescriptor.getCompanionObjectDescriptor();
        if (companionObjectDescriptor != null) {
            builder.setCompanionObjectName(getSimpleNameIndex(companionObjectDescriptor.getName()));
        }

        ProtoBuf.TypeTable typeTableProto = typeTable.serialize();
        if (typeTableProto != null) {
            builder.setTypeTable(typeTableProto);
        }

        extension.serializeClass(classDescriptor, builder);

        return builder;
    }

    @NotNull
    public ProtoBuf.Property.Builder propertyProto(@NotNull PropertyDescriptor descriptor) {
        ProtoBuf.Property.Builder builder = ProtoBuf.Property.newBuilder();

        DescriptorSerializer local = createChildSerializer(descriptor);

        boolean hasGetter = false;
        boolean hasSetter = false;
        boolean lateInit = descriptor.isLateInit();
        boolean isConst = descriptor.isConst();

        ConstantValue<?> compileTimeConstant = descriptor.getCompileTimeInitializer();
        boolean hasConstant = !(compileTimeConstant == null || compileTimeConstant instanceof NullValue);

        boolean hasAnnotations = !descriptor.getAnnotations().getAllAnnotations().isEmpty();

        int propertyFlags = Flags.getAccessorFlags(
                hasAnnotations,
                descriptor.getVisibility(),
                descriptor.getModality(),
                false,
                false
        );

        PropertyGetterDescriptor getter = descriptor.getGetter();
        if (getter != null) {
            hasGetter = true;
            int accessorFlags = getAccessorFlags(getter);
            if (accessorFlags != propertyFlags) {
                builder.setGetterFlags(accessorFlags);
            }
        }

        PropertySetterDescriptor setter = descriptor.getSetter();
        if (setter != null) {
            hasSetter = true;
            int accessorFlags = getAccessorFlags(setter);
            if (accessorFlags != propertyFlags) {
                builder.setSetterFlags(accessorFlags);
            }

            if (!setter.isDefault()) {
                DescriptorSerializer setterLocal = local.createChildSerializer(setter);
                for (ValueParameterDescriptor valueParameterDescriptor : setter.getValueParameters()) {
                    builder.setSetterValueParameter(setterLocal.valueParameter(valueParameterDescriptor));
                }
            }
        }

        int flags = Flags.getPropertyFlags(
                hasAnnotations, descriptor.getVisibility(), descriptor.getModality(), descriptor.getKind(), descriptor.isVar(),
                hasGetter, hasSetter, hasConstant, isConst, lateInit
        );
        if (flags != builder.getFlags()) {
            builder.setFlags(flags);
        }

        builder.setName(getSimpleNameIndex(descriptor.getName()));

        if (useTypeTable()) {
            builder.setReturnTypeId(local.typeId(descriptor.getType()));
        }
        else {
            builder.setReturnType(local.type(descriptor.getType()));
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor));
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.setReceiverTypeId(local.typeId(receiverParameter.getType()));
            }
            else {
                builder.setReceiverType(local.type(receiverParameter.getType()));
            }
        }

        extension.serializeProperty(descriptor, builder);

        return builder;
    }

    @NotNull
    public ProtoBuf.Function.Builder functionProto(@NotNull FunctionDescriptor descriptor) {
        ProtoBuf.Function.Builder builder = ProtoBuf.Function.newBuilder();

        DescriptorSerializer local = createChildSerializer(descriptor);

        int flags = Flags.getFunctionFlags(
                hasAnnotations(descriptor), descriptor.getVisibility(), descriptor.getModality(), descriptor.getKind(),
                descriptor.isOperator(), descriptor.isInfix(), descriptor.isInline(), descriptor.isTailrec(),
                descriptor.isExternal()
        );
        if (flags != builder.getFlags()) {
            builder.setFlags(flags);
        }

        builder.setName(getSimpleNameIndex(descriptor.getName()));

        if (useTypeTable()) {
            //noinspection ConstantConditions
            builder.setReturnTypeId(local.typeId(descriptor.getReturnType()));
        }
        else {
            //noinspection ConstantConditions
            builder.setReturnType(local.type(descriptor.getReturnType()));
        }

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor));
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.setReceiverTypeId(local.typeId(receiverParameter.getType()));
            }
            else {
                builder.setReceiverType(local.type(receiverParameter.getType()));
            }
        }

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
        }

        if (serializeTypeTableToFunction) {
            ProtoBuf.TypeTable typeTableProto = typeTable.serialize();
            if (typeTableProto != null) {
                builder.setTypeTable(typeTableProto);
            }
        }

        extension.serializeFunction(descriptor, builder);

        return builder;
    }

    @NotNull
    public ProtoBuf.Constructor.Builder constructorProto(@NotNull ConstructorDescriptor descriptor) {
        ProtoBuf.Constructor.Builder builder = ProtoBuf.Constructor.newBuilder();

        DescriptorSerializer local = createChildSerializer(descriptor);

        int flags = Flags.getConstructorFlags(hasAnnotations(descriptor), descriptor.getVisibility(), !descriptor.isPrimary());
        if (flags != builder.getFlags()) {
            builder.setFlags(flags);
        }

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
        }

        extension.serializeConstructor(descriptor, builder);

        return builder;
    }

    @NotNull
    public ProtoBuf.EnumEntry.Builder enumEntryProto(@NotNull ClassDescriptor descriptor) {
        ProtoBuf.EnumEntry.Builder builder = ProtoBuf.EnumEntry.newBuilder();
        builder.setName(getSimpleNameIndex(descriptor.getName()));
        extension.serializeEnumEntry(descriptor, builder);
        return builder;
    }

    private static int getAccessorFlags(@NotNull PropertyAccessorDescriptor accessor) {
        return Flags.getAccessorFlags(
                hasAnnotations(accessor),
                accessor.getVisibility(),
                accessor.getModality(),
                !accessor.isDefault(),
                accessor.isExternal()
        );
    }

    @NotNull
    private ProtoBuf.ValueParameter.Builder valueParameter(@NotNull ValueParameterDescriptor descriptor) {
        ProtoBuf.ValueParameter.Builder builder = ProtoBuf.ValueParameter.newBuilder();

        int flags = Flags.getValueParameterFlags(hasAnnotations(descriptor), descriptor.declaresDefaultValue(),
                                                 descriptor.isCrossinline(), descriptor.isNoinline());
        if (flags != builder.getFlags()) {
            builder.setFlags(flags);
        }

        builder.setName(getSimpleNameIndex(descriptor.getName()));

        if (useTypeTable()) {
            builder.setTypeId(typeId(descriptor.getType()));
        }
        else {
            builder.setType(type(descriptor.getType()));
        }

        KotlinType varargElementType = descriptor.getVarargElementType();
        if (varargElementType != null) {
            if (useTypeTable()) {
                builder.setVarargElementTypeId(typeId(varargElementType));
            }
            else {
                builder.setVarargElementType(type(varargElementType));
            }
        }

        extension.serializeValueParameter(descriptor, builder);

        return builder;
    }

    private ProtoBuf.TypeParameter.Builder typeParameter(TypeParameterDescriptor typeParameter) {
        ProtoBuf.TypeParameter.Builder builder = ProtoBuf.TypeParameter.newBuilder();

        builder.setId(getTypeParameterId(typeParameter));

        builder.setName(getSimpleNameIndex(typeParameter.getName()));

        if (typeParameter.isReified() != builder.getReified()) {
            builder.setReified(typeParameter.isReified());
        }

        ProtoBuf.TypeParameter.Variance variance = variance(typeParameter.getVariance());
        if (variance != builder.getVariance()) {
            builder.setVariance(variance);
        }
        extension.serializeTypeParameter(typeParameter, builder);

        List<KotlinType> upperBounds = typeParameter.getUpperBounds();
        if (upperBounds.size() == 1 && KotlinBuiltIns.isDefaultBound(CollectionsKt.single(upperBounds))) return builder;

        for (KotlinType upperBound : upperBounds) {
            if (useTypeTable()) {
                builder.addUpperBoundId(typeId(upperBound));
            }
            else {
                builder.addUpperBound(type(upperBound));
            }
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
                return ProtoBuf.TypeParameter.Variance.OUT;
        }
        throw new IllegalStateException("Unknown variance: " + variance);
    }

    private int typeId(@NotNull KotlinType type) {
        return typeTable.get(type(type));
    }

    @NotNull
    private ProtoBuf.Type.Builder type(@NotNull KotlinType type) {
        ProtoBuf.Type.Builder builder = ProtoBuf.Type.newBuilder();

        if (type.isError()) {
            extension.serializeErrorType(type, builder);
            return builder;
        }

        if (FlexibleTypesKt.isFlexible(type)) {
            Flexibility flexibility = FlexibleTypesKt.flexibility(type);

            ProtoBuf.Type.Builder lowerBound = type(flexibility.getLowerBound());
            lowerBound.setFlexibleTypeCapabilitiesId(getStringTable().getStringIndex(flexibility.getFactory().getId()));
            if (useTypeTable()) {
                lowerBound.setFlexibleUpperBoundId(typeId(flexibility.getUpperBound()));
            }
            else {
                lowerBound.setFlexibleUpperBound(type(flexibility.getUpperBound()));
            }
            return lowerBound;
        }

        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor instanceof ClassDescriptor) {
            PossiblyInnerType possiblyInnerType = TypeParameterUtilsKt.buildPossiblyInnerType(type);
            assert possiblyInnerType != null : "possiblyInnerType should not be null in case of class";

            fillFromPossiblyInnerType(builder, possiblyInnerType);

        }
        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) descriptor;
            if (typeParameter.getContainingDeclaration() == containingDeclaration) {
                builder.setTypeParameterName(getSimpleNameIndex(typeParameter.getName()));
            }
            else {
                builder.setTypeParameter(getTypeParameterId(typeParameter));
            }

            assert type.getArguments().isEmpty() : "Found arguments for type constructor build on type parameter: " + descriptor;
        }

        if (type.isMarkedNullable() != builder.getNullable()) {
            builder.setNullable(type.isMarkedNullable());
        }

        extension.serializeType(type, builder);

        return builder;
    }

    private void fillFromPossiblyInnerType(
            @NotNull ProtoBuf.Type.Builder builder,
            @NotNull PossiblyInnerType type
    ) {
        builder.setClassName(getClassId(type.getClassDescriptor()));

        for (TypeProjection projection : type.getArguments()) {
            builder.addArgument(typeArgument(projection));
        }

        if (type.getOuterType() != null) {
            ProtoBuf.Type.Builder outerBuilder = ProtoBuf.Type.newBuilder();
            fillFromPossiblyInnerType(outerBuilder, type.getOuterType());
            if (useTypeTable()) {
                builder.setOuterTypeId(typeTable.get(outerBuilder));
            }
            else {
                builder.setOuterType(outerBuilder);
            }

        }
    }

    @NotNull
    private ProtoBuf.Type.Argument.Builder typeArgument(@NotNull TypeProjection typeProjection) {
        ProtoBuf.Type.Argument.Builder builder = ProtoBuf.Type.Argument.newBuilder();

        if (typeProjection.isStarProjection()) {
            builder.setProjection(ProtoBuf.Type.Argument.Projection.STAR);
        }
        else {
            ProtoBuf.Type.Argument.Projection projection = projection(typeProjection.getProjectionKind());

            if (projection != builder.getProjection()) {
                builder.setProjection(projection);
            }

            if (useTypeTable()) {
                builder.setTypeId(typeId(typeProjection.getType()));
            }
            else {
                builder.setType(type(typeProjection.getType()));
            }
        }

        return builder;
    }

    @NotNull
    public ProtoBuf.Package.Builder packageProto(@NotNull Collection<PackageFragmentDescriptor> fragments) {
        return packageProto(fragments, null);
    }

    @NotNull
    public ProtoBuf.Package.Builder packageProto(
            @NotNull Collection<PackageFragmentDescriptor> fragments,
            @Nullable Function1<DeclarationDescriptor, Boolean> skip
    ) {
        ProtoBuf.Package.Builder builder = ProtoBuf.Package.newBuilder();

        Collection<DeclarationDescriptor> members = new ArrayList<DeclarationDescriptor>();
        for (PackageFragmentDescriptor fragment : fragments) {
            members.addAll(DescriptorUtils.getAllDescriptors(fragment.getMemberScope()));
        }

        for (DeclarationDescriptor declaration : sort(members)) {
            if (skip != null && skip.invoke(declaration)) continue;

            if (declaration instanceof PropertyDescriptor) {
                builder.addProperty(propertyProto((PropertyDescriptor) declaration));
            }
            else if (declaration instanceof FunctionDescriptor) {
                builder.addFunction(functionProto((FunctionDescriptor) declaration));
            }
        }

        ProtoBuf.TypeTable typeTableProto = typeTable.serialize();
        if (typeTableProto != null) {
            builder.setTypeTable(typeTableProto);
        }

        extension.serializePackage(builder);

        return builder;
    }

    @NotNull
    public ProtoBuf.Package.Builder packagePartProto(@NotNull Collection<DeclarationDescriptor> members) {
        ProtoBuf.Package.Builder builder = ProtoBuf.Package.newBuilder();

        for (DeclarationDescriptor declaration : sort(members)) {
            if (declaration instanceof PropertyDescriptor) {
                builder.addProperty(propertyProto((PropertyDescriptor) declaration));
            }
            else if (declaration instanceof FunctionDescriptor) {
                builder.addFunction(functionProto((FunctionDescriptor) declaration));
            }
        }

        ProtoBuf.TypeTable typeTableProto = typeTable.serialize();
        if (typeTableProto != null) {
            builder.setTypeTable(typeTableProto);
        }

        extension.serializePackage(builder);

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
        return getStringTable().getFqNameIndex(descriptor);
    }

    private int getSimpleNameIndex(@NotNull Name name) {
        return getStringTable().getStringIndex(name.asString());
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
        //NOTE: the exact comparator does matter here
        Collections.sort(result, MemberComparator.INSTANCE);
        return result;

    }
}
