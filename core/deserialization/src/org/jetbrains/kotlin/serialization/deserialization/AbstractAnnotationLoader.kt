/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

abstract class AbstractAnnotationLoader<out A : Any>(
    protected val protocol: SerializerExtensionProtocol,
) : AnnotationLoader<A> {
    override fun loadClassAnnotations(container: ProtoContainer.Class): List<A> =
        loadAnnotations(
            container.classProto.annotationList,
            container.classProto.getExtension(protocol.classAnnotation),
            container.nameResolver,
        )

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind,
    ): List<A> = when (proto) {
        is ProtoBuf.Constructor ->
            loadAnnotations(
                proto.annotationList,
                proto.getExtension(protocol.constructorAnnotation),
                container.nameResolver,
            )
        is ProtoBuf.Function ->
            loadAnnotations(
                proto.annotationList,
                proto.getExtension(protocol.functionAnnotation),
                container.nameResolver,
            )
        is ProtoBuf.Property -> when (kind) {
            AnnotatedCallableKind.PROPERTY ->
                loadAnnotations(
                    proto.annotationList,
                    proto.getExtension(protocol.propertyAnnotation),
                    container.nameResolver,
                )
            AnnotatedCallableKind.PROPERTY_GETTER ->
                loadAnnotations(
                    proto.getterAnnotationList,
                    proto.getExtension(protocol.propertyGetterAnnotation),
                    container.nameResolver,
                )
            AnnotatedCallableKind.PROPERTY_SETTER ->
                loadAnnotations(
                    proto.setterAnnotationList,
                    proto.getExtension(protocol.propertySetterAnnotation),
                    container.nameResolver,
                )
            else -> error("Unsupported callable kind with property proto")
        }
        else -> error("Unknown message: $proto")
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> =
        loadAnnotations(
            proto.backingFieldAnnotationList,
            protocol.propertyBackingFieldAnnotation?.let { proto.getExtension(it) },
            container.nameResolver,
        )

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> =
        loadAnnotations(
            proto.delegateFieldAnnotationList,
            protocol.propertyDelegatedFieldAnnotation?.let { proto.getExtension(it) },
            container.nameResolver,
        )

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<A> =
        loadAnnotations(
            proto.annotationList,
            proto.getExtension(protocol.enumEntryAnnotation),
            container.nameResolver,
        )

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A> =
        loadAnnotations(
            proto.annotationList,
            proto.getExtension(protocol.parameterAnnotation),
            container.nameResolver,
        )

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind,
    ): List<A> = when (proto) {
        is ProtoBuf.Function ->
            loadAnnotations(
                proto.extensionReceiverAnnotationList,
                protocol.functionExtensionReceiverAnnotation?.let { proto.getExtension(it) },
                container.nameResolver,
            )
        is ProtoBuf.Property -> when (kind) {
            AnnotatedCallableKind.PROPERTY, AnnotatedCallableKind.PROPERTY_GETTER, AnnotatedCallableKind.PROPERTY_SETTER ->
                loadAnnotations(
                    proto.extensionReceiverAnnotationList,
                    protocol.propertyExtensionReceiverAnnotation?.let { proto.getExtension(it) },
                    container.nameResolver,
                )
            else -> error("Unsupported callable kind with property proto for receiver annotations: $kind")
        }
        else -> error("Unknown message: $proto")
    }

    override fun loadContextParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter?,
    ): List<A> = proto?.let {
        loadValueParameterAnnotations(container, callableProto, kind, parameterIndex, it)
    }.orEmpty()

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<A> =
        loadAnnotations(proto.annotationList, proto.getExtension(protocol.typeAnnotation), nameResolver)

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<A> =
        loadAnnotations(proto.annotationList, proto.getExtension(protocol.typeParameterAnnotation), nameResolver)

    private fun loadAnnotations(
        commonMetadataSource: List<ProtoBuf.Annotation>,
        extensionProtocolMetadataSource: List<ProtoBuf.Annotation>?,
        nameResolver: NameResolver,
    ) = commonMetadataSource.ifEmpty { extensionProtocolMetadataSource.orEmpty() }.map { loadAnnotation(it, nameResolver) }
}
