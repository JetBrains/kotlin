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
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.storage.StorageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable;
import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter;
import static org.jetbrains.jet.descriptors.serialization.TypeDeserializer.TypeParameterResolver.NONE;
import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;

public class DescriptorDeserializer {

    @NotNull
    public static DescriptorDeserializer create(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull Deserializers annotationDeserializer
    ) {
        return new DescriptorDeserializer(storageManager,
                new TypeDeserializer(storageManager, null, nameResolver, descriptorFinder,
                                     "Deserializer for " + containingDeclaration.getName(), NONE),
                containingDeclaration, nameResolver, annotationDeserializer);
    }

    @NotNull
    public static DescriptorDeserializer create(
            @NotNull StorageManager storageManager,
            @NotNull TypeDeserializer typeDeserializer,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver,
            @NotNull Deserializers annotationDeserializer
    ) {
        return new DescriptorDeserializer(storageManager, typeDeserializer, containingDeclaration, nameResolver, annotationDeserializer);
    }

    private final DeclarationDescriptor containingDeclaration;
    private final NameResolver nameResolver;
    private final TypeDeserializer typeDeserializer;
    private final Deserializers deserializers;

    private final StorageManager storageManager;

    private DescriptorDeserializer(
            @NotNull StorageManager storageManager,
            @NotNull TypeDeserializer typeDeserializer,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver,
            @NotNull Deserializers deserializers
    ) {
        this.storageManager = storageManager;
        this.typeDeserializer = typeDeserializer;
        this.containingDeclaration = containingDeclaration;
        this.nameResolver = nameResolver;
        this.deserializers = deserializers;
    }

    @NotNull
    public TypeDeserializer getTypeDeserializer() {
        return typeDeserializer;
    }

    @NotNull
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    public DescriptorDeserializer createChildDeserializer(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull final List<TypeParameter> typeParameterProtos,
            @NotNull final List<TypeParameterDescriptor> typeParameters
    ) {
        TypeDeserializer childTypeDeserializer = new TypeDeserializer(
                storageManager,
                typeDeserializer, "Child deserializer for " + descriptor.getName(),
                new TypeDeserializer.TypeParameterResolver() {
                    @NotNull
                    @Override
                    public List<DeserializedTypeParameterDescriptor> getTypeParameters(@NotNull TypeDeserializer typeDeserializer) {
                        List<DeserializedTypeParameterDescriptor> descriptors = typeParameters(typeParameterProtos, typeDeserializer);
                        typeParameters.addAll(descriptors);
                        return descriptors;
                    }
                });
        return create(storageManager, childTypeDeserializer, descriptor, nameResolver, deserializers);
    }

    @NotNull
    public CallableMemberDescriptor loadCallable(@NotNull Callable proto) {
        Callable.CallableKind callableKind = Flags.CALLABLE_KIND.get(proto.getFlags());
        switch (callableKind) {
            case FUN:
                return loadFunction(proto);
            case VAL:
            case VAR:
                return loadProperty(proto);
            case CONSTRUCTOR:
                return loadConstructor(proto);
        }
        throw new IllegalArgumentException("Unsupported callable kind: " + callableKind);
    }

    @NotNull
    private PropertyDescriptor loadProperty(@NotNull Callable proto) {
        int flags = proto.getFlags();

        PropertyDescriptorImpl property = new PropertyDescriptorImpl(
                containingDeclaration,
                getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
                modality(Flags.MODALITY.get(flags)),
                visibility(Flags.VISIBILITY.get(flags)),
                Flags.CALLABLE_KIND.get(flags) == Callable.CallableKind.VAR,
                nameResolver.getName(proto.getName()),
                memberKind(Flags.MEMBER_KIND.get(flags))
        );

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DescriptorDeserializer local = createChildDeserializer(property, proto.getTypeParameterList(), typeParameters);
        property.setType(
                local.typeDeserializer.type(proto.getReturnType()),
                typeParameters,
                getExpectedThisObject(),
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null)
        );

        PropertyGetterDescriptorImpl getter = null;
        PropertySetterDescriptorImpl setter = null;

        if (Flags.HAS_GETTER.get(flags)) {
            int getterFlags = proto.getGetterFlags();
            boolean isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags);
            if (isNotDefault) {
                getter = new PropertyGetterDescriptorImpl(
                        property, getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER),
                        modality(Flags.MODALITY.get(getterFlags)), visibility(Flags.VISIBILITY.get(getterFlags)),
                        isNotDefault, !isNotDefault, property.getKind()
                );
            }
            else {
                getter = DescriptorFactory.createDefaultGetter(property);
            }
            getter.initialize(property.getReturnType());
        }

        if (Flags.HAS_SETTER.get(flags)) {
            int setterFlags = proto.getSetterFlags();
            boolean isNotDefault = proto.hasSetterFlags() && Flags.IS_NOT_DEFAULT.get(setterFlags);
            if (isNotDefault) {
                setter = new PropertySetterDescriptorImpl(
                        property, getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER),
                        modality(Flags.MODALITY.get(setterFlags)), visibility(Flags.VISIBILITY.get(setterFlags)),
                        isNotDefault, !isNotDefault, property.getKind()
                );
                DescriptorDeserializer setterLocal = local.createChildDeserializer(setter, Collections.<TypeParameter>emptyList(),
                                                                                   Collections.<TypeParameterDescriptor>emptyList());
                List<ValueParameterDescriptor> valueParameters = setterLocal.valueParameters(proto, AnnotatedCallableKind.PROPERTY_SETTER);
                assert valueParameters.size() == 1 : "Property setter should have a single value parameter: " + setter;
                setter.initialize(valueParameters.get(0));
            }
            else {
                setter = DescriptorFactory.createDefaultSetter(property);
            }
        }

        property.initialize(getter, setter);

        return property;
    }

    @NotNull
    private CallableMemberDescriptor loadFunction(@NotNull Callable proto) {
        int flags = proto.getFlags();
        DeserializedSimpleFunctionDescriptor function = new DeserializedSimpleFunctionDescriptor(
                containingDeclaration, proto,
                deserializers,
                nameResolver
        );
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DescriptorDeserializer local = createChildDeserializer(function, proto.getTypeParameterList(), typeParameters);
        function.initialize(
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                getExpectedThisObject(),
                typeParameters,
                local.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                local.typeDeserializer.type(proto.getReturnType()),
                modality(Flags.MODALITY.get(flags)),
                visibility(Flags.VISIBILITY.get(flags))
        );
        return function;
    }

    @Nullable
    private ReceiverParameterDescriptor getExpectedThisObject() {
        return containingDeclaration instanceof ClassDescriptor
               ? ((ClassDescriptor) containingDeclaration).getThisAsReceiverParameter() : null;
    }

    @NotNull
    private CallableMemberDescriptor loadConstructor(@NotNull Callable proto) {
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
        ConstructorDescriptorImpl descriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION),
                // TODO: primary
                true);
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DescriptorDeserializer local = createChildDeserializer(descriptor, Collections.<TypeParameter>emptyList(), typeParameters);
        descriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                local.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                visibility(Flags.VISIBILITY.get(proto.getFlags())),
                DescriptorUtils.isConstructorOfStaticNestedClass(descriptor)
        );
        descriptor.setReturnType(local.typeDeserializer.type(proto.getReturnType()));
        return descriptor;
    }

    @NotNull
    private Annotations getAnnotations(@NotNull Callable proto, int flags, @NotNull AnnotatedCallableKind kind) {
        return getAnnotations(containingDeclaration, proto, flags, kind, deserializers.getAnnotationDeserializer(), nameResolver);
    }

    public static Annotations getAnnotations(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Callable proto,
            int flags,
            @NotNull AnnotatedCallableKind kind,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull NameResolver nameResolver
    ) {
        assert containingDeclaration instanceof ClassOrPackageFragmentDescriptor
                : "Only members in classes or package fragments should be serialized: " + containingDeclaration;
        return Flags.HAS_ANNOTATIONS.get(flags)
               ? annotationDeserializer
                       .loadCallableAnnotations((ClassOrPackageFragmentDescriptor) containingDeclaration, proto, nameResolver, kind)
               : Annotations.EMPTY;
    }

    public static CallableMemberDescriptor.Kind memberKind(Callable.MemberKind memberKind) {
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
    public List<DeserializedTypeParameterDescriptor> typeParameters(
            @NotNull List<TypeParameter> protos,
            @NotNull TypeDeserializer typeDeserializer
    ) {
        List<DeserializedTypeParameterDescriptor> result = new ArrayList<DeserializedTypeParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            TypeParameter proto = protos.get(i);
            DeserializedTypeParameterDescriptor descriptor = new DeserializedTypeParameterDescriptor(
                    storageManager,
                    typeDeserializer,
                    proto,
                    containingDeclaration,
                    nameResolver.getName(proto.getName()),
                    variance(proto.getVariance()),
                    proto.getReified(),
                    i
            );
            result.add(descriptor);
        }
        return result;
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
    private List<ValueParameterDescriptor> valueParameters(@NotNull Callable callable, @NotNull AnnotatedCallableKind kind) {
        DeclarationDescriptor containerOfCallable = containingDeclaration.getContainingDeclaration();
        assert containerOfCallable instanceof ClassOrPackageFragmentDescriptor
                : "Only members in classes or package fragments should be serialized: " + containerOfCallable;
        ClassOrPackageFragmentDescriptor classOrPackage = (ClassOrPackageFragmentDescriptor) containerOfCallable;

        List<Callable.ValueParameter> protos = callable.getValueParameterList();
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            Callable.ValueParameter proto = protos.get(i);
            result.add(new ValueParameterDescriptorImpl(
                    containingDeclaration,
                    i,
                    getAnnotations(classOrPackage, callable, kind, proto),
                    nameResolver.getName(proto.getName()),
                    typeDeserializer.type(proto.getType()),
                    Flags.DECLARES_DEFAULT_VALUE.get(proto.getFlags()),
                    typeDeserializer.typeOrNull(proto.hasVarargElementType() ? proto.getVarargElementType() : null))
            );
        }
        return result;
    }

    @NotNull
    private Annotations getAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor classOrPackage,
            @NotNull Callable callable,
            @NotNull AnnotatedCallableKind kind,
            @NotNull Callable.ValueParameter valueParameter
    ) {
        return Flags.HAS_ANNOTATIONS.get(valueParameter.getFlags())
               ? deserializers.getAnnotationDeserializer().loadValueParameterAnnotations(classOrPackage, callable, nameResolver, kind, valueParameter)
               : Annotations.EMPTY;
    }
}
