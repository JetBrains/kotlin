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
    private final DeclarationDescriptor containingDeclaration;
    private final NameResolver nameResolver;
    private final TypeDeserializer typeDeserializer;

    public DescriptorDeserializer(
            @Nullable DescriptorDeserializer parent,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        TypeDeserializer parentTypeDeserializer = parent == null ? null : parent.typeDeserializer;
        this.typeDeserializer = new TypeDeserializer(parentTypeDeserializer, nameResolver);
        this.containingDeclaration = containingDeclaration;
        this.nameResolver = nameResolver;
    }

    @NotNull
    private DescriptorDeserializer createChildDeserializer(@NotNull DeclarationDescriptor descriptor) {
        return new DescriptorDeserializer(this, descriptor, nameResolver);
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
            supertypes.add(local.typeDeserializer.type(supertype));
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
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                // TODO: expectedThisObject
                null,
                local.typeParameters(proto.getTypeParametersList()),
                local.valueParameters(proto.getValueParametersList()),
                local.typeDeserializer.type(proto.getReturnType()),
                // TODO: modality
                Modality.OPEN,
                // TODO: visibility
                Visibilities.PUBLIC,
                // TODO: inline
                false

        );
        return function;
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
        typeDeserializer.registerTypeParameter(id, descriptor);
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
                typeDeserializer.type(proto.getType()),
                // TODO: declaresDefaultValue
                false,
                typeDeserializer.typeOrNull(proto.hasVarargElementType() ? proto.getVarargElementType() : null));
    }
}
