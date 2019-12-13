/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.common

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.types.KotlinType

class AnnotationLoaderForStubBuilderImpl(
    private val protocol: SerializerExtensionProtocol
) : AnnotationAndConstantLoader<ClassId, Unit> {

    override fun loadClassAnnotations(container: ProtoContainer.Class): List<ClassId> =
        container.classProto.getExtension(protocol.classAnnotation).orEmpty().map { container.nameResolver.getClassId(it.id) }

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<ClassId> {
        val annotations = when (proto) {
            is ProtoBuf.Constructor -> proto.getExtension(protocol.constructorAnnotation)
            is ProtoBuf.Function -> proto.getExtension(protocol.functionAnnotation)
            is ProtoBuf.Property -> when (kind) {
                AnnotatedCallableKind.PROPERTY -> proto.getExtension(protocol.propertyAnnotation)
                AnnotatedCallableKind.PROPERTY_GETTER -> proto.getExtension(protocol.propertyGetterAnnotation)
                AnnotatedCallableKind.PROPERTY_SETTER -> proto.getExtension(protocol.propertySetterAnnotation)
                else -> error("Unsupported callable kind with property proto")
            }
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { container.nameResolver.getClassId(it.id) }
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<ClassId> =
        emptyList()

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<ClassId> =
        emptyList()

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<ClassId> =
        proto.getExtension(protocol.enumEntryAnnotation).orEmpty().map { container.nameResolver.getClassId(it.id) }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<ClassId> =
        proto.getExtension(protocol.parameterAnnotation).orEmpty().map { container.nameResolver.getClassId(it.id) }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<ClassId> = emptyList()

    override fun loadTypeAnnotations(
        proto: ProtoBuf.Type,
        nameResolver: NameResolver
    ): List<ClassId> =
        proto.getExtension(protocol.typeAnnotation).orEmpty().map { nameResolver.getClassId(it.id) }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<ClassId> =
        proto.getExtension(protocol.typeParameterAnnotation).orEmpty().map { nameResolver.getClassId(it.id) }

    override fun loadPropertyConstant(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: KotlinType
    ) {
    }
}
