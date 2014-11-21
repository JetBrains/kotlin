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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext
import org.jetbrains.jet.descriptors.serialization.descriptors.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.impl.*
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable.CallableKind.*
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter
import java.util.ArrayList

public class MemberDeserializer(private val context: DeserializationContext) {
    private val components: DeserializationComponents get() = context.components

    public fun loadCallable(proto: Callable): CallableMemberDescriptor {
        val callableKind = Flags.CALLABLE_KIND.get(proto.getFlags())
        return when (callableKind) {
            FUN -> loadFunction(proto)
            VAL, VAR -> loadProperty(proto)
            CONSTRUCTOR -> loadConstructor(proto)
            else -> throw IllegalArgumentException("Unsupported callable kind: $callableKind")
        }
    }

    private fun loadProperty(proto: Callable): PropertyDescriptor {
        val flags = proto.getFlags()

        val property = DeserializedPropertyDescriptor(
                context.containingDeclaration, null,
                getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
                modality(Flags.MODALITY.get(flags)),
                visibility(Flags.VISIBILITY.get(flags)),
                Flags.CALLABLE_KIND.get(flags) == Callable.CallableKind.VAR,
                context.nameResolver.getName(proto.getName()),
                memberKind(Flags.MEMBER_KIND.get(flags)),
                proto,
                context.nameResolver
        )

        val local = context.childContext(property, proto.getTypeParameterList())
        property.setType(
                local.typeDeserializer.type(proto.getReturnType()),
                local.typeDeserializer.getOwnTypeParameters(),
                getDispatchReceiverParameter(),
                local.typeDeserializer.typeOrNull(if (proto.hasReceiverType()) proto.getReceiverType() else null)
        )

        val getter = if (Flags.HAS_GETTER.get(flags)) {
            val getterFlags = proto.getGetterFlags()
            val isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags)
            val getter = if (isNotDefault) {
                PropertyGetterDescriptorImpl(
                        property,
                        getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER),
                        modality(Flags.MODALITY.get(getterFlags)),
                        visibility(Flags.VISIBILITY.get(getterFlags)),
                        /* hasBody = */ isNotDefault,
                        /* isDefault = */ !isNotDefault,
                        property.getKind(), null, SourceElement.NO_SOURCE
                )
            }
            else {
                DescriptorFactory.createDefaultGetter(property)
            }
            getter.initialize(property.getReturnType())
            getter
        }
        else {
            null
        }

        val setter = if (Flags.HAS_SETTER.get(flags)) {
            val setterFlags = proto.getSetterFlags()
            val isNotDefault = proto.hasSetterFlags() && Flags.IS_NOT_DEFAULT.get(setterFlags)
            if (isNotDefault) {
                val setter = PropertySetterDescriptorImpl(
                        property,
                        getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER),
                        modality(Flags.MODALITY.get(setterFlags)),
                        visibility(Flags.VISIBILITY.get(setterFlags)),
                        /* hasBody = */ isNotDefault,
                        /* isDefault = */ !isNotDefault,
                        property.getKind(), null, SourceElement.NO_SOURCE
                )
                val setterLocal = local.childContext(setter, listOf())
                val valueParameters = setterLocal.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.PROPERTY_SETTER)
                assert(valueParameters.size() == 1) { "Property setter should have a single value parameter: $setter" }
                setter.initialize(valueParameters.single())
                setter
            }
            else {
                DescriptorFactory.createDefaultSetter(property)
            }
        }
        else {
            null
        }

        if (Flags.HAS_CONSTANT.get(flags)) {
            property.setCompileTimeInitializer(
                components.storageManager.createNullableLazyValue {
                    val containingDeclaration =
                            context.containingDeclaration as? ClassOrPackageFragmentDescriptor
                            ?: error("Only members in classes or package fragments should be serialized: ${context.containingDeclaration}")
                    components.constantLoader.loadPropertyConstant(
                            containingDeclaration, proto, context.nameResolver, AnnotatedCallableKind.PROPERTY
                    )
                }
            )
        }

        property.initialize(getter, setter)

        return property
    }

    private fun loadFunction(proto: Callable): CallableMemberDescriptor {
        val annotations = getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION)
        val function = DeserializedSimpleFunctionDescriptor.create(context.containingDeclaration, proto, context.nameResolver, annotations)
        val local = context.childContext(function, proto.getTypeParameterList())
        function.initialize(
                local.typeDeserializer.typeOrNull(if (proto.hasReceiverType()) proto.getReceiverType() else null),
                getDispatchReceiverParameter(),
                local.typeDeserializer.getOwnTypeParameters(),
                local.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                local.typeDeserializer.type(proto.getReturnType()),
                modality(Flags.MODALITY.get(proto.getFlags())),
                visibility(Flags.VISIBILITY.get(proto.getFlags()))
        )
        return function
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (context.containingDeclaration as? ClassDescriptor)?.getThisAsReceiverParameter()
    }

    private fun loadConstructor(proto: Callable): CallableMemberDescriptor {
        val classDescriptor = context.containingDeclaration as ClassDescriptor
        val descriptor = ConstructorDescriptorImpl.create(
                classDescriptor, getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION), // TODO: primary
                true, SourceElement.NO_SOURCE
        )
        val local = context.childContext(descriptor, listOf())
        descriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                local.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                visibility(Flags.VISIBILITY.get(proto.getFlags()))
        )
        descriptor.setReturnType(local.typeDeserializer.type(proto.getReturnType()))
        return descriptor
    }

    private fun getAnnotations(proto: Callable, flags: Int, kind: AnnotatedCallableKind): Annotations {
        val containingDeclaration =
                context.containingDeclaration as? ClassOrPackageFragmentDescriptor
                ?: error("Only members in classes or package fragments should be serialized: ${context.containingDeclaration}")
        return if (Flags.HAS_ANNOTATIONS.get(flags))
            components.annotationLoader.loadCallableAnnotations(containingDeclaration, proto, context.nameResolver, kind)
        else
            Annotations.EMPTY
    }

    public fun typeParameters(protos: List<TypeParameter>): List<DeserializedTypeParameterDescriptor> {
        val result = ArrayList<DeserializedTypeParameterDescriptor>(protos.size())
        for (i in protos.indices) {
            val proto = protos.get(i)
            result.add(DeserializedTypeParameterDescriptor(
                    components.storageManager, context.typeDeserializer, proto, context.containingDeclaration,
                    context.nameResolver.getName(proto.getName()),
                    variance(proto.getVariance()), proto.getReified(), i
            ))
        }
        return result
    }

    private fun valueParameters(callable: Callable, kind: AnnotatedCallableKind): List<ValueParameterDescriptor> {
        val classOrPackage =
                context.containingDeclaration.getContainingDeclaration() as? ClassOrPackageFragmentDescriptor
                ?: error("Only members in classes or package fragments should be serialized: ${context.containingDeclaration}")

        val protos = callable.getValueParameterList()
        val result = ArrayList<ValueParameterDescriptor>(protos.size())
        for (i in protos.indices) {
            val proto = protos.get(i)
            result.add(ValueParameterDescriptorImpl(
                    context.containingDeclaration, null, i,
                    getParameterAnnotations(classOrPackage, callable, kind, proto),
                    context.nameResolver.getName(proto.getName()),
                    context.typeDeserializer.type(proto.getType()),
                    Flags.DECLARES_DEFAULT_VALUE.get(proto.getFlags()),
                    context.typeDeserializer.typeOrNull(if (proto.hasVarargElementType()) proto.getVarargElementType() else null),
                    SourceElement.NO_SOURCE
            ))
        }
        return result
    }

    private fun getParameterAnnotations(
            classOrPackage: ClassOrPackageFragmentDescriptor,
            callable: Callable,
            kind: AnnotatedCallableKind,
            valueParameter: Callable.ValueParameter
    ): Annotations {
        return if (Flags.HAS_ANNOTATIONS.get(valueParameter.getFlags()))
            components.annotationLoader.loadValueParameterAnnotations(classOrPackage, callable, context.nameResolver, kind, valueParameter)
        else
            Annotations.EMPTY
    }
}
