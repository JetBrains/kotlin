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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;

public class DescriptorSerializer {

    // TODO: flags

    private final NameTable nameTable;
    private final Interner<TypeParameterDescriptor> typeParameters;

    public DescriptorSerializer() {
        this(new NameTable(), new Interner<TypeParameterDescriptor>());
    }

    private DescriptorSerializer(NameTable nameTable, Interner<TypeParameterDescriptor> typeParameters) {
        this.nameTable = nameTable;
        this.typeParameters = typeParameters;
    }

    private DescriptorSerializer createChildSerializer() {
        return new DescriptorSerializer(nameTable, new Interner<TypeParameterDescriptor>(typeParameters));
    }

    @NotNull
    public NameTable getNameTable() {
        return nameTable;
    }

    @NotNull
    public ProtoBuf.Class.Builder classProto(@NotNull ClassDescriptor classDescriptor) {
        ProtoBuf.Class.Builder builder = ProtoBuf.Class.newBuilder();

        // TODO annotations

        int flags = Flags.getClassFlags(classDescriptor.getVisibility(), classDescriptor.getModality(), classDescriptor.getKind(),
                                        classDescriptor.isInner());
        builder.setFlags(flags);

        // TODO extra visibility

        builder.setName(nameTable.getSimpleNameIndex(classDescriptor.getName()));

        DescriptorSerializer local = createChildSerializer();

        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            builder.addTypeParameters(local.typeParameter(typeParameterDescriptor));
        }

        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
            builder.addSupertypes(local.type(supertype));
        }

        // TODO: nested classes
        // TODO: constructor
        // TODO: class object

        for (DeclarationDescriptor descriptor : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            // TODO: other than functions
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor function = (FunctionDescriptor) descriptor;
                if (function.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue;
                builder.addMembers(local.functionProto(function));
            }
        }

        return builder;
    }

    @NotNull
    public ProtoBuf.Callable.Builder functionProto(@NotNull FunctionDescriptor function) {
        ProtoBuf.Callable.Builder builder = ProtoBuf.Callable.newBuilder();

        // TODO setter flags
        builder.setFlags(Flags.getCallableFlags(function.getVisibility(),
                                                function.getModality(),
                                                function.getKind(),
                                                callableKind(function),
                                                function instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) function).isInline())
        );
        //TODO builder.setExtraVisibility()

        //TODO builder.addAnnotations()

        DescriptorSerializer local = this;//createChildSerializer();

        for (TypeParameterDescriptor typeParameterDescriptor : function.getTypeParameters()) {
            builder.addTypeParameters(local.typeParameter(typeParameterDescriptor));
        }

        ReceiverParameterDescriptor receiverParameter = function.getReceiverParameter();
        if (receiverParameter != null) {
            builder.setReceiverType(local.type(receiverParameter.getType()));
        }

        builder.setName(nameTable.getSimpleNameIndex(function.getName()));

        for (ValueParameterDescriptor valueParameterDescriptor : function.getValueParameters()) {
            builder.addValueParameters(local.valueParameter(valueParameterDescriptor));
        }

        builder.setReturnType(local.type(function.getReturnType()));

        return builder;
    }

    private static ProtoBuf.Callable.CallableKind callableKind(CallableMemberDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            return propertyDescriptor.isVar() ? ProtoBuf.Callable.CallableKind.VAR : ProtoBuf.Callable.CallableKind.VAL;
        }
        if (descriptor instanceof ConstructorDescriptor) {
            return ProtoBuf.Callable.CallableKind.CONSTRUCTOR;
        }
        assert descriptor instanceof FunctionDescriptor : "Unknown descriptor class: " + descriptor.getClass();
        return ProtoBuf.Callable.CallableKind.FUN;
    }

    private ProtoBuf.Callable.ValueParameter.Builder valueParameter(ValueParameterDescriptor descriptor) {
        ProtoBuf.Callable.ValueParameter.Builder builder = ProtoBuf.Callable.ValueParameter.newBuilder();

        builder.setFlags(flags(descriptor));

        builder.setName(nameTable.getSimpleNameIndex(descriptor.getName()));

        builder.setType(type(descriptor.getType()));

        JetType varargElementType = descriptor.getVarargElementType();
        if (varargElementType != null) {
            builder.setVarargElementType(type(varargElementType));
        }

        return builder;
    }

    private int flags(ValueParameterDescriptor descriptor) {
        // TODO
        return 0;
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
            builder.addUpperBounds(type(upperBound));
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
        ProtoBuf.Type.Builder builder = ProtoBuf.Type.newBuilder();

        builder.setConstructor(typeConstructor(type.getConstructor()));

        for (TypeProjection projection : type.getArguments()) {
            builder.addArguments(typeArgument(projection));
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
}
