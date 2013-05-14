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

import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DescriptorDeserializer {
    @Nullable
    private final DescriptorDeserializer parent;
    private final DeclarationDescriptor containingDeclaration;
    private final NameResolver nameResolver;
    private final TIntObjectHashMap<TypeParameterDescriptorImpl> typeParameterDescriptors = new TIntObjectHashMap<TypeParameterDescriptorImpl>();

    public DescriptorDeserializer(
            @Nullable DescriptorDeserializer parent,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        this.parent = parent;
        this.containingDeclaration = containingDeclaration;
        this.nameResolver = nameResolver;
    }

    @NotNull
    private DescriptorDeserializer createChildDeserializer(@NotNull DeclarationDescriptor descriptor) {
        return new DescriptorDeserializer(this, descriptor, nameResolver);
    }

    private void registerTypeParameter(int id, @NotNull TypeParameterDescriptorImpl descriptor) {
        typeParameterDescriptors.put(id, descriptor);
    }

    @Nullable
    private TypeParameterDescriptorImpl getTypeParameter(int id) {
        TypeParameterDescriptorImpl descriptor = typeParameterDescriptors.get(id);
        if (descriptor == null && parent != null) {
            return parent.getTypeParameter(id);
        }
        return descriptor;
    }

    @NotNull
    public ClassDescriptor loadClass(@NotNull ProtoBuf.Class proto) {
        MutableClassDescriptor descriptor = new MutableClassDescriptor(
                containingDeclaration,
                // TODO: outerScope
                JetScope.EMPTY,
                // TODO: kind
                ClassKind.CLASS,
                // TODO: isInner
                false,
                nameResolver.getName(proto.getName())
        );


        // TODO: visibility
        descriptor.setVisibility(Visibilities.INTERNAL);

        // TODO: modality
        descriptor.setModality(Modality.OPEN);

        DescriptorDeserializer local = createChildDeserializer(descriptor);

        descriptor.setTypeParameterDescriptors(local.typeParameters(proto.getTypeParametersList()));

        // TODO: descriptor.setPrimaryConstructor();

        // TODO: descriptor.setAnnotations();

        WritableScopeImpl members = new WritableScopeImpl(JetScope.EMPTY, descriptor, RedeclarationHandler.DO_NOTHING,
                                                         "Members of " + descriptor.getName());
        descriptor.setScopeForMemberLookup(members);

        for (ProtoBuf.Callable callable : proto.getMembersList()) {
            // TODO: properties, classes etc
            members.addFunctionDescriptor(local.loadFunction(callable));
        }
        members.changeLockLevel(WritableScope.LockLevel.READING);

        List<JetType> supertypes = new ArrayList<JetType>(proto.getSupertypesCount());
        for (ProtoBuf.Type supertype : proto.getSupertypesList()) {
            supertypes.add(local.type(supertype));
        }
        descriptor.setSupertypes(supertypes);

        descriptor.createTypeConstructor();

        return descriptor;
    }

    @NotNull
    public FunctionDescriptor loadFunction(@NotNull ProtoBuf.Callable proto) {
        // TODO: assert function flag

        SimpleFunctionDescriptorImpl function = new SimpleFunctionDescriptorImpl(
                containingDeclaration,
                // TODO: annotations
                Collections.<AnnotationDescriptor>emptyList(),
                nameResolver.getName(proto.getName()),
                // TODO: kind
                CallableMemberDescriptor.Kind.DECLARATION
        );
        DescriptorDeserializer local = new DescriptorDeserializer(this, function, nameResolver);
        function.initialize(
                local.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                // TODO: expectedThisObject
                null,
                local.typeParameters(proto.getTypeParametersList()),
                local.valueParameters(proto.getValueParametersList()),
                local.type(proto.getReturnType()),
                // TODO: modality
                Modality.OPEN,
                // TODO: visibility
                Visibilities.PUBLIC,
                // TODO: inline
                false

        );
        return function;
    }

    @Nullable
    private JetType typeOrNull(@Nullable ProtoBuf.Type proto) {
        if (proto == null) {
            return null;
        }
        return type(proto);
    }

    @NotNull
    private JetType type(@NotNull ProtoBuf.Type proto) {
        ProtoBuf.Type.Constructor constructorProto = proto.getConstructor();
        int id = constructorProto.getId();
        TypeConstructor typeConstructor = typeConstructor(constructorProto);
        if (typeConstructor == null) {
            String message = constructorProto.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS
                             ? nameResolver.getFqName(id).getFqName()
                             : "Unknown type parameter " + id;
            return ErrorUtils.createErrorType(message);
        }

        List<TypeProjection> typeArguments = typeArguments(proto.getArgumentsList());
        return new JetTypeImpl(
            Collections.<AnnotationDescriptor>emptyList(),
            typeConstructor,
            proto.getNullable(),
            typeArguments,
            getTypeMemberScope(typeConstructor, typeArguments)
        );
    }

    @NotNull
    private static JetScope getTypeMemberScope(@NotNull TypeConstructor constructor, @NotNull List<TypeProjection> typeArguments) {
        ClassifierDescriptor descriptor = constructor.getDeclarationDescriptor();
        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            return typeParameterDescriptor.getDefaultType().getMemberScope();
        }
        return ((ClassDescriptor) descriptor).getMemberScope(typeArguments);
    }

    @Nullable
    private TypeConstructor typeConstructor(@NotNull ProtoBuf.Type.Constructor proto) {
        switch (proto.getKind()) {
            case CLASS:
                ClassDescriptor classDescriptor = nameResolver.getClassDescriptor(proto.getId());
                if (classDescriptor == null) return null;

                return classDescriptor.getTypeConstructor();
            case TYPE_PARAMETER:
                TypeParameterDescriptorImpl descriptor = getTypeParameter(proto.getId());
                if (descriptor == null) return null;

                return descriptor.getTypeConstructor();
        }
        throw new IllegalStateException("Unknown kind " + proto.getKind());
    }

    private List<TypeProjection> typeArguments(List<ProtoBuf.Type.Argument> protos) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(protos.size());
        for (ProtoBuf.Type.Argument proto : protos) {
            result.add(typeProjection(proto));
        }
        return result;
    }

    private TypeProjection typeProjection(ProtoBuf.Type.Argument proto) {
        return new TypeProjection(
                variance(proto.getProjection()),
                type(proto.getType())
        );
    }

    private static Variance variance(ProtoBuf.Type.Argument.Projection proto) {
        switch (proto) {
            case IN:
                return Variance.IN_VARIANCE;
            case OUT:
                return Variance.OUT_VARIANCE;
            case INV:
                return Variance.INVARIANT;
        }
        throw new IllegalStateException("Unknown projection: " + proto);
    }

    @NotNull
    private List<TypeParameterDescriptor> typeParameters(@NotNull List<ProtoBuf.TypeParameter> protos) {
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            ProtoBuf.TypeParameter proto = protos.get(i);
            TypeParameterDescriptorImpl descriptor = typeParameter(proto, i);
            result.add(descriptor);
        }
        return result;
    }

    private TypeParameterDescriptorImpl typeParameter(ProtoBuf.TypeParameter proto, int index) {
        int id = proto.getId();
        TypeParameterDescriptorImpl descriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDeclaration,
                // TODO
                Collections.<AnnotationDescriptor>emptyList(),
                proto.getReified(),
                variance(proto.getVariance()),
                nameResolver.getName(proto.getName()),
                index);
        registerTypeParameter(id, descriptor);
        // TODO: circular bounds
        descriptor.setInitialized();
        return descriptor;
    }

    private Variance variance(ProtoBuf.TypeParameter.Variance proto) {
        switch (proto) {
            case IN:
                return Variance.IN_VARIANCE;
            case OUT:
                return Variance.OUT_VARIANCE;
            case INV:
                return Variance.INVARIANT;
        }
        throw new IllegalStateException("Unknown projection: " + proto);
    }

    @NotNull
    private List<ValueParameterDescriptor> valueParameters(@NotNull List<ProtoBuf.Callable.ValueParameter> protos) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            ProtoBuf.Callable.ValueParameter proto = protos.get(i);
            result.add(valueParameter(proto, i));
        }
        return result;
    }

    private ValueParameterDescriptor valueParameter(ProtoBuf.Callable.ValueParameter proto, int index) {
        return new ValueParameterDescriptorImpl(
                containingDeclaration,
                index,
                // TODO
                Collections.<AnnotationDescriptor>emptyList(),
                nameResolver.getName(proto.getName()),
                type(proto.getType()),
                // TODO: declaresDefaultValue
                false,
                typeOrNull(proto.hasVarargElementType() ? proto.getVarargElementType() : null));
    }
}
