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

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContextWithTypes;
import org.jetbrains.jet.descriptors.serialization.descriptors.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyGetterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertySetterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable;
import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter;
import static org.jetbrains.jet.descriptors.serialization.SerializationPackage.*;

public class MemberDeserializer {

    private final DeserializationContextWithTypes context;

    public MemberDeserializer(@NotNull DeserializationContextWithTypes context) {
        this.context = context;
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
    private PropertyDescriptor loadProperty(@NotNull final Callable proto) {
        final int flags = proto.getFlags();

        DeserializedPropertyDescriptor property = new DeserializedPropertyDescriptor(
                context.getContainingDeclaration(),
                null,
                getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
                modality(Flags.MODALITY.get(flags)),
                visibility(Flags.VISIBILITY.get(flags)),
                Flags.CALLABLE_KIND.get(flags) == Callable.CallableKind.VAR,
                context.getNameResolver().getName(proto.getName()),
                SerializationPackage.memberKind(Flags.MEMBER_KIND.get(flags)),
                proto,
                context.getNameResolver()
        );

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DeserializationContextWithTypes local = context.childContext(property, proto.getTypeParameterList(), typeParameters);
        property.setType(
                local.getTypeDeserializer().type(proto.getReturnType()),
                typeParameters,
                getExpectedThisObject(),
                local.getTypeDeserializer().typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null)
        );

        PropertyGetterDescriptorImpl getter = null;
        PropertySetterDescriptorImpl setter = null;

        if (Flags.HAS_GETTER.get(flags)) {
            int getterFlags = proto.getGetterFlags();
            boolean isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags);
            if (isNotDefault) {
                getter = new PropertyGetterDescriptorImpl(property,
                                                          getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER),
                                                          modality(Flags.MODALITY.get(getterFlags)),
                                                          visibility(Flags.VISIBILITY.get(getterFlags)),
                                                          isNotDefault, !isNotDefault,
                                                          property.getKind(), null, SourceElement.NO_SOURCE);
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
                setter = new PropertySetterDescriptorImpl(property,
                                                          getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER),
                                                          modality(Flags.MODALITY.get(setterFlags)),
                                                          visibility(Flags.VISIBILITY.get(setterFlags)), isNotDefault,
                                                          !isNotDefault,
                                                          property.getKind(), null, SourceElement.NO_SOURCE);
                DeserializationContextWithTypes setterLocal = local.childContext(setter, Collections.<TypeParameter>emptyList(),
                                                                                 Collections.<TypeParameterDescriptor>emptyList());
                List<ValueParameterDescriptor> valueParameters
                        = setterLocal.getDeserializer().valueParameters(proto, AnnotatedCallableKind.PROPERTY_SETTER);
                assert valueParameters.size() == 1 : "Property setter should have a single value parameter: " + setter;
                setter.initialize(valueParameters.get(0));
            }
            else {
                setter = DescriptorFactory.createDefaultSetter(property);
            }
        }

        if (Flags.HAS_CONSTANT.get(flags)) {
            property.setCompileTimeInitializer(
                    context.getStorageManager().createNullableLazyValue(new Function0<CompileTimeConstant<?>>() {
                        @Nullable
                        @Override
                        public CompileTimeConstant<?> invoke() {
                            DeclarationDescriptor containingDeclaration = context.getContainingDeclaration();
                            assert containingDeclaration instanceof ClassOrPackageFragmentDescriptor
                                    : "Only members in classes or package fragments should be serialized: " + containingDeclaration;
                            return context.getConstantLoader().loadPropertyConstant(
                                    (ClassOrPackageFragmentDescriptor) containingDeclaration, proto,
                                    context.getNameResolver(), AnnotatedCallableKind.PROPERTY);
                        }
                    })
            );
        }

        property.initialize(getter, setter);

        return property;
    }

    @NotNull
    private CallableMemberDescriptor loadFunction(@NotNull Callable proto) {
        int flags = proto.getFlags();
        DeserializedSimpleFunctionDescriptor function = DeserializedSimpleFunctionDescriptor.create(
                context.getContainingDeclaration(), proto, context.getAnnotationLoader(), context.getNameResolver()
        );
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DeserializationContextWithTypes local = context.childContext(function, proto.getTypeParameterList(), typeParameters);
        function.initialize(
                local.getTypeDeserializer().typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                getExpectedThisObject(),
                typeParameters,
                local.getDeserializer().valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                local.getTypeDeserializer().type(proto.getReturnType()),
                modality(Flags.MODALITY.get(flags)),
                visibility(Flags.VISIBILITY.get(flags))
        );
        return function;
    }

    @Nullable
    private ReceiverParameterDescriptor getExpectedThisObject() {
        DeclarationDescriptor containingDeclaration = context.getContainingDeclaration();
        return containingDeclaration instanceof ClassDescriptor
               ? ((ClassDescriptor) containingDeclaration).getThisAsReceiverParameter() : null;
    }

    @NotNull
    private CallableMemberDescriptor loadConstructor(@NotNull Callable proto) {
        ClassDescriptor classDescriptor = (ClassDescriptor) context.getContainingDeclaration();
        ConstructorDescriptorImpl descriptor = ConstructorDescriptorImpl.create(
                classDescriptor,
                getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION),
                // TODO: primary
                true, SourceElement.NO_SOURCE);
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(proto.getTypeParameterCount());
        DeserializationContextWithTypes local = context.childContext(descriptor, Collections.<TypeParameter>emptyList(), typeParameters);
        descriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                local.getDeserializer().valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                visibility(Flags.VISIBILITY.get(proto.getFlags()))
        );
        descriptor.setReturnType(local.getTypeDeserializer().type(proto.getReturnType()));
        return descriptor;
    }

    @NotNull
    private Annotations getAnnotations(@NotNull Callable proto, int flags, @NotNull AnnotatedCallableKind kind) {
        return getAnnotations(context.getContainingDeclaration(), proto, flags, kind, context.getAnnotationLoader(), context.getNameResolver());
    }

    public static Annotations getAnnotations(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Callable proto,
            int flags,
            @NotNull AnnotatedCallableKind kind,
            @NotNull AnnotationLoader annotationLoader,
            @NotNull NameResolver nameResolver
    ) {
        assert containingDeclaration instanceof ClassOrPackageFragmentDescriptor
                : "Only members in classes or package fragments should be serialized: " + containingDeclaration;
        return Flags.HAS_ANNOTATIONS.get(flags) ? annotationLoader.loadCallableAnnotations(
                (ClassOrPackageFragmentDescriptor) containingDeclaration, proto, nameResolver, kind) : Annotations.EMPTY;
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
                    context.getStorageManager(),
                    typeDeserializer,
                    proto,
                    context.getContainingDeclaration(),
                    context.getNameResolver().getName(proto.getName()),
                    variance(proto.getVariance()),
                    proto.getReified(),
                    i
            );
            result.add(descriptor);
        }
        return result;
    }

    @NotNull
    private List<ValueParameterDescriptor> valueParameters(@NotNull Callable callable, @NotNull AnnotatedCallableKind kind) {
        DeclarationDescriptor containerOfCallable = context.getContainingDeclaration().getContainingDeclaration();
        assert containerOfCallable instanceof ClassOrPackageFragmentDescriptor
                : "Only members in classes or package fragments should be serialized: " + containerOfCallable;
        ClassOrPackageFragmentDescriptor classOrPackage = (ClassOrPackageFragmentDescriptor) containerOfCallable;

        List<Callable.ValueParameter> protos = callable.getValueParameterList();
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            Callable.ValueParameter proto = protos.get(i);
            result.add(new ValueParameterDescriptorImpl(
                    context.getContainingDeclaration(),
                    null,
                    i,
                    getAnnotations(classOrPackage, callable, kind, proto),
                    context.getNameResolver().getName(proto.getName()),
                    context.getTypeDeserializer().type(proto.getType()),
                    Flags.DECLARES_DEFAULT_VALUE.get(proto.getFlags()),
                    context.getTypeDeserializer().typeOrNull(proto.hasVarargElementType() ? proto.getVarargElementType() : null),
                    SourceElement.NO_SOURCE)
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
        return Flags.HAS_ANNOTATIONS.get(valueParameter.getFlags()) ? context.getAnnotationLoader()
                       .loadValueParameterAnnotations(classOrPackage, callable, context.getNameResolver(), kind, valueParameter)
               : Annotations.EMPTY;
    }
}
