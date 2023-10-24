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
    override fun loadClassAnnotations(container: ProtoContainer.Class): List<A> {
        val annotations = container.classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { proto -> loadAnnotation(proto, container.nameResolver) }
    }

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A> {
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
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> {
        val annotations = protocol.propertyBackingFieldAnnotation?.let { proto.getExtension(it) }.orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<A> {
        val annotations = protocol.propertyDelegatedFieldAnnotation?.let { proto.getExtension(it) }.orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<A> {
        val annotations = proto.getExtension(protocol.enumEntryAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A> {
        val annotations = proto.getExtension(protocol.parameterAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A> {
        val annotations = when (proto) {
            is ProtoBuf.Function -> protocol.functionExtensionReceiverAnnotation?.let { proto.getExtension(it) }
            is ProtoBuf.Property -> when (kind) {
                AnnotatedCallableKind.PROPERTY, AnnotatedCallableKind.PROPERTY_GETTER, AnnotatedCallableKind.PROPERTY_SETTER -> {
                    protocol.propertyExtensionReceiverAnnotation?.let { proto.getExtension(it) }
                }
                else -> error("Unsupported callable kind with property proto for receiver annotations: $kind")
            }
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { annotationProto ->
            loadAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<A> {
        return proto.getExtension(protocol.typeAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<A> {
        return proto.getExtension(protocol.typeParameterAnnotation).orEmpty().map { loadAnnotation(it, nameResolver) }
    }
}
