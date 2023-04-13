/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import org.jetbrains.kotlin.analysis.decompiler.stub.AnnotationWithArgs
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.stubs.impl.createConstantValue
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.*

class AnnotationLoaderForStubBuilderImpl(
    private val protocol: SerializerExtensionProtocol
) : AnnotationLoader<AnnotationWithArgs> {

    override fun loadClassAnnotations(container: ProtoContainer.Class): List<AnnotationWithArgs> =
        container.classProto.getExtension(protocol.classAnnotation).orEmpty()
            .map { loadAnnotation(it, container.nameResolver) }

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<AnnotationWithArgs> {
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
        return annotations.map { loadAnnotation(it, container.nameResolver) }
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<AnnotationWithArgs> =
        emptyList()

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<AnnotationWithArgs> =
        emptyList()

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<AnnotationWithArgs> =
        proto.getExtension(protocol.enumEntryAnnotation).orEmpty().map { loadAnnotation(it, container.nameResolver) }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<AnnotationWithArgs> =
        proto.getExtension(protocol.parameterAnnotation).orEmpty().map { loadAnnotation(it, container.nameResolver) }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<AnnotationWithArgs> = emptyList()

    override fun loadTypeAnnotations(
        proto: ProtoBuf.Type,
        nameResolver: NameResolver
    ): List<AnnotationWithArgs> =
        proto.getExtension(protocol.typeAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<AnnotationWithArgs> =
        proto.getExtension(protocol.typeParameterAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }

    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationWithArgs {
        val valueMap = proto.argumentList.associate { nameResolver.getName(it.nameId) to createConstantValue(it.value, nameResolver) }
        return AnnotationWithArgs(nameResolver.getClassId(proto.id), valueMap)
    }
}
