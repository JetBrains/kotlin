/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind.FUN
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind.VAL
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind.VAR
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.utils.toReadOnlyList

public class MemberDeserializer(private val c: DeserializationContext) {
    public fun loadCallable(proto: Callable): CallableMemberDescriptor {
        val callableKind = Flags.CALLABLE_KIND.get(proto.getFlags())
        return when (callableKind) {
            FUN -> loadFunction(proto)
            VAL, VAR -> loadProperty(proto)
            else -> throw IllegalArgumentException("Unsupported callable kind: $callableKind")
        }
    }

    private fun loadProperty(proto: Callable): PropertyDescriptor {
        val flags = proto.getFlags()

        val property = DeserializedPropertyDescriptor(
                c.containingDeclaration, null,
                getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
                Deserialization.modality(Flags.MODALITY.get(flags)),
                Deserialization.visibility(Flags.VISIBILITY.get(flags)),
                Flags.CALLABLE_KIND.get(flags) == Callable.CallableKind.VAR,
                c.nameResolver.getName(proto.getName()),
                Deserialization.memberKind(Flags.MEMBER_KIND.get(flags)),
                proto,
                c.nameResolver,
                Flags.LATE_INIT.get(flags),
                Flags.IS_CONST.get(flags)
        )

        val local = c.childContext(property, proto.getTypeParameterList())

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations = if (hasGetter)
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.PROPERTY_GETTER)
        else
            Annotations.EMPTY

        property.setType(
                local.typeDeserializer.type(proto.getReturnType()),
                local.typeDeserializer.ownTypeParameters,
                getDispatchReceiverParameter(),
                if (proto.hasReceiverType()) local.typeDeserializer.type(proto.getReceiverType(), receiverAnnotations) else null
        )

        val getter = if (hasGetter) {
            val getterFlags = proto.getGetterFlags()
            val isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags)
            val getter = if (isNotDefault) {
                PropertyGetterDescriptorImpl(
                        property,
                        getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER),
                        Deserialization.modality(Flags.MODALITY.get(getterFlags)),
                        Deserialization.visibility(Flags.VISIBILITY.get(getterFlags)),
                        /* hasBody = */ isNotDefault,
                        /* isDefault = */ !isNotDefault,
                        property.getKind(), null, SourceElement.NO_SOURCE
                )
            }
            else {
                DescriptorFactory.createDefaultGetter(property, Annotations.EMPTY)
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
                        Deserialization.modality(Flags.MODALITY.get(setterFlags)),
                        Deserialization.visibility(Flags.VISIBILITY.get(setterFlags)),
                        /* hasBody = */ isNotDefault,
                        /* isDefault = */ !isNotDefault,
                        property.getKind(), null, SourceElement.NO_SOURCE
                )
                val setterLocal = local.childContext(setter, listOf())
                val valueParameters = setterLocal.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.PROPERTY_SETTER)
                setter.initialize(valueParameters.single())
                setter
            }
            else {
                DescriptorFactory.createDefaultSetter(property, Annotations.EMPTY)
            }
        }
        else {
            null
        }

        if (Flags.HAS_CONSTANT.get(flags)) {
            property.setCompileTimeInitializer(
                    c.storageManager.createNullableLazyValue {
                        val container = c.containingDeclaration.asProtoContainer()!!
                        c.components.annotationAndConstantLoader.loadPropertyConstant(container, proto, c.nameResolver, property.getReturnType())
                    }
            )
        }

        property.initialize(getter, setter)

        return property
    }

    private fun loadFunction(proto: Callable): CallableMemberDescriptor {
        val annotations = getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION)
        val receiverAnnotations = getReceiverParameterAnnotations(proto, AnnotatedCallableKind.FUNCTION)
        val function = DeserializedSimpleFunctionDescriptor.create(c.containingDeclaration, proto, c.nameResolver, annotations)
        val local = c.childContext(function, proto.getTypeParameterList())
        function.initialize(
                if (proto.hasReceiverType()) local.typeDeserializer.type(proto.getReceiverType(), receiverAnnotations) else null,
                getDispatchReceiverParameter(),
                local.typeDeserializer.ownTypeParameters,
                local.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                local.typeDeserializer.type(proto.returnType),
                Deserialization.modality(Flags.MODALITY.get(proto.flags)),
                Deserialization.visibility(Flags.VISIBILITY.get(proto.flags)),
                Flags.IS_OPERATOR.get(proto.flags)
        )
        return function
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (c.containingDeclaration as? ClassDescriptor)?.getThisAsReceiverParameter()
    }

    public fun loadConstructor(proto: Callable, isPrimary: Boolean): ConstructorDescriptor {
        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedConstructorDescriptor(
                classDescriptor, null, getAnnotations(proto, proto.getFlags(), AnnotatedCallableKind.FUNCTION),
                isPrimary, CallableMemberDescriptor.Kind.DECLARATION, proto, c.nameResolver
        )
        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                local.memberDeserializer.valueParameters(proto, AnnotatedCallableKind.FUNCTION),
                Deserialization.visibility(Flags.VISIBILITY.get(proto.getFlags()))
        )
        descriptor.setReturnType(local.typeDeserializer.type(proto.getReturnType()))
        return descriptor
    }

    private fun getAnnotations(proto: Callable, flags: Int, kind: AnnotatedCallableKind): Annotations {
        if (!Flags.HAS_ANNOTATIONS.get(flags)) {
            return Annotations.EMPTY
        }
        return DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader.loadCallableAnnotations(
                        it, proto, c.nameResolver, kind
                )
            }.orEmpty()
        }
    }

    private fun getReceiverParameterAnnotations(
            proto: Callable,
            kind: AnnotatedCallableKind,
            receiverTargetedKind: AnnotatedCallableKind = kind
    ): Annotations {
        return DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            if (proto.hasReceiverType()) {
                c.containingDeclaration.asProtoContainer()?.let {
                    c.components.annotationAndConstantLoader
                            .loadExtensionReceiverParameterAnnotations(it, proto, c.nameResolver, receiverTargetedKind)
                            .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
                }.orEmpty()
            }
            else emptyList()
        }
    }

    private fun valueParameters(callable: Callable, kind: AnnotatedCallableKind): List<ValueParameterDescriptor> {
        val callableDescriptor = c.containingDeclaration as CallableDescriptor
        val containerOfCallable = callableDescriptor.containingDeclaration.asProtoContainer()

        return callable.valueParameterList.mapIndexed { i, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            ValueParameterDescriptorImpl(
                    callableDescriptor, null, i,
                    containerOfCallable?.let { getParameterAnnotations(it, callable, kind, i, proto) } ?: Annotations.EMPTY,
                    c.nameResolver.getName(proto.name),
                    c.typeDeserializer.type(proto.type),
                    Flags.DECLARES_DEFAULT_VALUE.get(flags),
                    if (proto.hasVarargElementType()) c.typeDeserializer.type(proto.varargElementType) else null,
                    SourceElement.NO_SOURCE
            )
        }.toReadOnlyList()
    }

    private fun getParameterAnnotations(
            container: ProtoContainer,
            callable: Callable,
            kind: AnnotatedCallableKind,
            index: Int,
            valueParameter: Callable.ValueParameter
    ): Annotations {
        return DeserializedAnnotations(c.storageManager) {
            c.components.annotationAndConstantLoader.loadValueParameterAnnotations(
                    container, callable, c.nameResolver, kind, index, valueParameter
            )
        }
    }

    private fun DeclarationDescriptor.asProtoContainer(): ProtoContainer? = when(this) {
        is PackageFragmentDescriptor -> ProtoContainer(null, fqName)
        is DeserializedClassDescriptor -> ProtoContainer(classProto, null)
        else -> null // TODO: support annotations on lambdas and their parameters
    }
}
