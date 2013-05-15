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
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.types.Variance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.*;

public class DescriptorDeserializer {
    private final DeclarationDescriptor containingDeclaration;
    private final NameResolver nameResolver;
    private final TypeDeserializer typeDeserializer;

    public DescriptorDeserializer(
            @NotNull TypeDeserializer typeDeserializer,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        this.typeDeserializer = typeDeserializer;
        this.containingDeclaration = containingDeclaration;
        this.nameResolver = nameResolver;
    }

    public DescriptorDeserializer(
            @Nullable DescriptorDeserializer parent,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        this(
                new TypeDeserializer(parent == null ? null : parent.typeDeserializer, nameResolver),
                containingDeclaration,
                nameResolver
        );
    }

    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    public TypeDeserializer getTypeDeserializer() {
        return typeDeserializer;
    }

    @NotNull
    private DescriptorDeserializer createChildDeserializer(@NotNull DeclarationDescriptor descriptor) {
        return new DescriptorDeserializer(this, descriptor, nameResolver);
    }

    @NotNull
    public CallableMemberDescriptor loadCallable(@NotNull Callable proto) {
        Callable.CallableKind callableKind = Flags.getCallableKind(proto.getFlags());
        switch (callableKind) {
            case FUN:
                return loadFunction(proto);
            case VAL:
            case VAR:
                return loadProperty(proto);
            case CONSTRUCTOR:
                break;
        }
        throw new IllegalArgumentException("Unsupported callable kind: " + callableKind);
    }

    @NotNull
    private PropertyDescriptor loadProperty(@NotNull Callable proto) {
        int flags = proto.getFlags();
        PropertyDescriptorImpl property = new PropertyDescriptorImpl(
                containingDeclaration,
                // TODO: annotations
                Collections.<AnnotationDescriptor>emptyList(),
                modality(Flags.getModality(flags)),
                visibility(Flags.getVisibility(flags)),
                Flags.getCallableKind(flags) == Callable.CallableKind.VAR,
                nameResolver.getName(proto.getName()),
                memberKind(Flags.getMemberKind(flags))
        );
        DescriptorDeserializer local = new DescriptorDeserializer(this, property, nameResolver);
        List<TypeParameterDescriptor> typeParameters = local.typeParameters(proto.getTypeParametersList());
        property.setType(
                local.typeDeserializer.type(proto.getReturnType()),
                typeParameters,
                // TODO: expected this object
                null,
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null)
        );
        return property;
    }

    @NotNull
    private CallableMemberDescriptor loadFunction(@NotNull Callable proto) {
        int flags = proto.getFlags();
        SimpleFunctionDescriptorImpl function = new SimpleFunctionDescriptorImpl(
                containingDeclaration,
                // TODO: annotations
                Collections.<AnnotationDescriptor>emptyList(),
                nameResolver.getName(proto.getName()),
                memberKind(Flags.getMemberKind(flags))
        );
        DescriptorDeserializer local = new DescriptorDeserializer(this, function, nameResolver);
        List<TypeParameterDescriptor> typeParameters = local.typeParameters(proto.getTypeParametersList());
        function.initialize(
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                // TODO: expectedThisObject
                null,
                typeParameters,
                local.valueParameters(proto.getValueParametersList()),
                local.typeDeserializer.type(proto.getReturnType()),
                modality(Flags.getModality(flags)),
                visibility(Flags.getVisibility(flags)),
                Flags.isInline(flags)

        );
        return function;
    }

    private static CallableMemberDescriptor.Kind memberKind(Callable.MemberKind memberKind) {
        switch (memberKind) {
            case DECLARATION:
                return CallableMemberDescriptor.Kind.DECLARATION;
            case FAKE_OVERRIDE:
                return CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
            case DELEGATION:
                return CallableMemberDescriptor.Kind.DELEGATION;
            case SYNTHESIZED:
                return CallableMemberDescriptor.Kind.SYNTHESIZED;
        }
        throw new IllegalArgumentException("Unknown member kind: " + memberKind);
    }

    @NotNull
    public static Modality modality(@NotNull ProtoBuf.Modality modality) {
        switch (modality) {
            case FINAL:
                return Modality.FINAL;
            case OPEN:
                return Modality.OPEN;
            case ABSTRACT:
                return Modality.ABSTRACT;
        }
        throw new IllegalArgumentException("Unknown modality: " + modality);
    }

    @NotNull
    public static Visibility visibility(@NotNull ProtoBuf.Visibility visibility) {
        switch (visibility) {
            case INTERNAL:
                return Visibilities.INTERNAL;
            case PRIVATE:
                return Visibilities.PRIVATE;
            case PROTECTED:
                return Visibilities.PROTECTED;
            case PUBLIC:
                return Visibilities.PUBLIC;
            case EXTRA:
                throw new UnsupportedOperationException("Extra visibilities are not supported yet"); // TODO
        }
        throw new IllegalArgumentException("Unknown visibility: " + visibility);
    }

    @NotNull
    public static ClassKind classKind(@NotNull ProtoBuf.Class.Kind kind) {
        switch (kind) {
            case CLASS:
                return ClassKind.CLASS;
            case TRAIT:
                return ClassKind.TRAIT;
            case ENUM_CLASS:
                return ClassKind.ENUM_CLASS;
            case ENUM_ENTRY:
                return ClassKind.ENUM_ENTRY;
            case ANNOTATION_CLASS:
                return ClassKind.ANNOTATION_CLASS;
            case OBJECT:
                return ClassKind.OBJECT;
            case CLASS_OBJECT:
                return ClassKind.CLASS_OBJECT;
        }
        throw new IllegalArgumentException("Unknown class kind: " + kind);
    }

    @NotNull
    public List<TypeParameterDescriptor> typeParameters(@NotNull List<TypeParameter> protos) {
        List<TypeParameterDescriptorImpl> result = new ArrayList<TypeParameterDescriptorImpl>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            TypeParameter proto = protos.get(i);
            TypeParameterDescriptorImpl descriptor = typeParameter(proto, i);
            result.add(descriptor);
        }
        // Account for circular bounds:
        for (int i = 0; i < protos.size(); i++) {
            TypeParameter proto = protos.get(i);
            TypeParameterDescriptorImpl descriptor = result.get(i);
            addTypeParameterBounds(proto, descriptor);
        }
        //noinspection unchecked
        return (List) result;
    }

    private TypeParameterDescriptorImpl typeParameter(TypeParameter proto, int index) {
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
        return descriptor;
    }

    private void addTypeParameterBounds(TypeParameter proto, TypeParameterDescriptorImpl descriptor) {
        for (Type upperBound : proto.getUpperBoundsList()) {
            descriptor.addUpperBound(typeDeserializer.type(upperBound));
        }
        descriptor.addDefaultUpperBound();
        descriptor.setInitialized();
    }

    private static Variance variance(TypeParameter.Variance proto) {
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
    private List<ValueParameterDescriptor> valueParameters(@NotNull List<Callable.ValueParameter> protos) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            Callable.ValueParameter proto = protos.get(i);
            result.add(valueParameter(proto, i));
        }
        return result;
    }

    private ValueParameterDescriptor valueParameter(Callable.ValueParameter proto, int index) {
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
