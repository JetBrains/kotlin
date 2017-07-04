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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*

class MemberDeserializer(private val c: DeserializationContext) {
    private val annotationDeserializer = AnnotationDeserializer(c.components.moduleDescriptor, c.components.notFoundClasses)
    fun loadProperty(proto: ProtoBuf.Property): PropertyDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val property = DeserializedPropertyDescriptor(
                c.containingDeclaration, null,
                getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
                Deserialization.modality(Flags.MODALITY.get(flags)),
                Deserialization.visibility(Flags.VISIBILITY.get(flags)),
                Flags.IS_VAR.get(flags),
                c.nameResolver.getName(proto.name),
                Deserialization.memberKind(Flags.MEMBER_KIND.get(flags)),
                Flags.IS_LATEINIT.get(flags),
                Flags.IS_CONST.get(flags),
                Flags.IS_EXTERNAL_PROPERTY.get(flags),
                Flags.IS_DELEGATED.get(flags),
                Flags.IS_HEADER_PROPERTY.get(flags),
                proto,
                c.nameResolver,
                c.typeTable,
                c.sinceKotlinInfoTable,
                c.containerSource
        )

        val local = c.childContext(property, proto.typeParameterList)

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations = if (hasGetter && proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.PROPERTY_GETTER)
        else
            Annotations.EMPTY

        property.setType(
                local.typeDeserializer.type(proto.returnType(c.typeTable)),
                local.typeDeserializer.ownTypeParameters,
                getDispatchReceiverParameter(),
                proto.receiverType(c.typeTable)?.let { local.typeDeserializer.type(it, receiverAnnotations) }
        )

        val getter = if (hasGetter) {
            val getterFlags = proto.getterFlags
            val isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags)
            val isExternal = proto.hasGetterFlags() && Flags.IS_EXTERNAL_ACCESSOR.get(getterFlags)
            val isInline = proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(getterFlags)
            val getter = if (isNotDefault) {
                PropertyGetterDescriptorImpl(
                        property,
                        getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER),
                        Deserialization.modality(Flags.MODALITY.get(getterFlags)),
                        Deserialization.visibility(Flags.VISIBILITY.get(getterFlags)),
                        /* isDefault = */ !isNotDefault,
                        /* isExternal = */ isExternal,
                        isInline,
                        property.kind, null, SourceElement.NO_SOURCE
                )
            }
            else {
                DescriptorFactory.createDefaultGetter(property, Annotations.EMPTY)
            }
            getter.initialize(property.returnType)
            getter
        }
        else {
            null
        }

        val setter = if (Flags.HAS_SETTER.get(flags)) {
            val setterFlags = proto.setterFlags
            val isNotDefault = proto.hasSetterFlags() && Flags.IS_NOT_DEFAULT.get(setterFlags)
            val isExternal = proto.hasSetterFlags() && Flags.IS_EXTERNAL_ACCESSOR.get(setterFlags)
            val isInline = proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(setterFlags)
            if (isNotDefault) {
                val setter = PropertySetterDescriptorImpl(
                        property,
                        getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER),
                        Deserialization.modality(Flags.MODALITY.get(setterFlags)),
                        Deserialization.visibility(Flags.VISIBILITY.get(setterFlags)),
                        /* isDefault = */ !isNotDefault,
                        /* isExternal = */ isExternal,
                        isInline,
                        property.kind, null, SourceElement.NO_SOURCE
                )
                val setterLocal = local.childContext(setter, listOf())
                val valueParameters = setterLocal.memberDeserializer.valueParameters(
                        listOf(proto.setterValueParameter), proto, AnnotatedCallableKind.PROPERTY_SETTER
                )
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
                        c.components.annotationAndConstantLoader.loadPropertyConstant(container, proto, property.returnType)
                    }
            )
        }

        property.initialize(getter, setter)

        return property
    }

    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadFunction(proto: ProtoBuf.Function): SimpleFunctionDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val annotations = getAnnotations(proto, flags, AnnotatedCallableKind.FUNCTION)
        val receiverAnnotations = if (proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.FUNCTION)
        else Annotations.EMPTY
        val function = DeserializedSimpleFunctionDescriptor(
                c.containingDeclaration, /* original = */ null, annotations, c.nameResolver.getName(proto.name),
                Deserialization.memberKind(Flags.MEMBER_KIND.get(flags)), proto, c.nameResolver, c.typeTable, c.sinceKotlinInfoTable,
                c.containerSource
        )
        val local = c.childContext(function, proto.typeParameterList)
        function.initialize(
                proto.receiverType(c.typeTable)?.let { local.typeDeserializer.type(it, receiverAnnotations) },
                getDispatchReceiverParameter(),
                local.typeDeserializer.ownTypeParameters,
                local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
                local.typeDeserializer.type(proto.returnType(c.typeTable)),
                Deserialization.modality(Flags.MODALITY.get(flags)),
                Deserialization.visibility(Flags.VISIBILITY.get(flags))
        )
        function.isOperator = Flags.IS_OPERATOR.get(flags)
        function.isInfix = Flags.IS_INFIX.get(flags)
        function.isExternal = Flags.IS_EXTERNAL_FUNCTION.get(flags)
        function.isInline = Flags.IS_INLINE.get(flags)
        function.isTailrec = Flags.IS_TAILREC.get(flags)
        function.isSuspend = Flags.IS_SUSPEND.get(flags)
        function.isHeader = Flags.IS_HEADER_FUNCTION.get(flags)
        return function
    }

    fun loadTypeAlias(proto: ProtoBuf.TypeAlias): TypeAliasDescriptor {
        val annotations = AnnotationsImpl(proto.annotationList.map { annotationDeserializer.deserializeAnnotation(it, c.nameResolver) })

        val visibility = Deserialization.visibility(Flags.VISIBILITY.get(proto.flags))
        val typeAlias = DeserializedTypeAliasDescriptor(
                c.storageManager, c.containingDeclaration, annotations, c.nameResolver.getName(proto.name),
                visibility, proto, c.nameResolver, c.typeTable, c.sinceKotlinInfoTable, c.containerSource
        )

        val local = c.childContext(typeAlias, proto.typeParameterList)
        typeAlias.initialize(
                local.typeDeserializer.ownTypeParameters,
                local.typeDeserializer.simpleType(proto.underlyingType(c.typeTable)),
                local.typeDeserializer.simpleType(proto.expandedType(c.typeTable))
        )

        return typeAlias
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (c.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
    }

    fun loadConstructor(proto: ProtoBuf.Constructor, isPrimary: Boolean): ClassConstructorDescriptor {
        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedClassConstructorDescriptor(
                classDescriptor, null, getAnnotations(proto, proto.flags, AnnotatedCallableKind.FUNCTION),
                isPrimary, CallableMemberDescriptor.Kind.DECLARATION, proto, c.nameResolver, c.typeTable, c.sinceKotlinInfoTable,
                c.containerSource
        )
        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
                local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
                Deserialization.visibility(Flags.VISIBILITY.get(proto.flags))
        )
        descriptor.returnType = classDescriptor.defaultType
        return descriptor
    }

    private fun getAnnotations(proto: MessageLite, flags: Int, kind: AnnotatedCallableKind): Annotations {
        if (!Flags.HAS_ANNOTATIONS.get(flags)) {
            return Annotations.EMPTY
        }
        return NonEmptyDeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader.loadCallableAnnotations(it, proto, kind).toList()
            }.orEmpty()
        }
    }

    private fun getReceiverParameterAnnotations(
            proto: MessageLite,
            kind: AnnotatedCallableKind,
            receiverTargetedKind: AnnotatedCallableKind = kind
    ): Annotations {
        return DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader
                        .loadExtensionReceiverParameterAnnotations(it, proto, receiverTargetedKind)
                        .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
                        .toList()
            }.orEmpty()
        }
    }

    private fun valueParameters(
            valueParameters: List<ProtoBuf.ValueParameter>,
            callable: MessageLite,
            kind: AnnotatedCallableKind
    ): List<ValueParameterDescriptor> {
        val callableDescriptor = c.containingDeclaration as CallableDescriptor
        val containerOfCallable = callableDescriptor.containingDeclaration.asProtoContainer()

        return valueParameters.mapIndexed { i, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            val annotations = if (containerOfCallable != null && Flags.HAS_ANNOTATIONS.get(flags)) {
                NonEmptyDeserializedAnnotations(c.storageManager) {
                    c.components.annotationAndConstantLoader
                            .loadValueParameterAnnotations(containerOfCallable, callable, kind, i, proto)
                            .toList()
                }
            }
            else Annotations.EMPTY
            ValueParameterDescriptorImpl(
                    callableDescriptor, null, i,
                    annotations,
                    c.nameResolver.getName(proto.name),
                    c.typeDeserializer.type(proto.type(c.typeTable)),
                    Flags.DECLARES_DEFAULT_VALUE.get(flags),
                    Flags.IS_CROSSINLINE.get(flags),
                    Flags.IS_NOINLINE.get(flags),
                    proto.varargElementType(c.typeTable)?.let { c.typeDeserializer.type(it) },
                    SourceElement.NO_SOURCE
            )
        }.toList()
    }

    private fun DeclarationDescriptor.asProtoContainer(): ProtoContainer? = when (this) {
        is PackageFragmentDescriptor -> ProtoContainer.Package(fqName, c.nameResolver, c.typeTable, c.containerSource)
        is DeserializedClassDescriptor -> thisAsProtoContainer
        else -> null // TODO: support annotations on lambdas and their parameters
    }
}
