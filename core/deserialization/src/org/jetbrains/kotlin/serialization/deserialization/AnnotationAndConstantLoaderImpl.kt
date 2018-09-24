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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.KotlinType

class AnnotationAndConstantLoaderImpl(
    module: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    private val protocol: SerializerExtensionProtocol
) : AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>> {
    private val deserializer = AnnotationDeserializer(module, notFoundClasses)

    override fun loadClassAnnotations(container: ProtoContainer.Class): List<AnnotationDescriptor> {
        val annotations = container.classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { proto -> deserializer.deserializeAnnotation(proto, container.nameResolver) }
    }

    override fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<AnnotationDescriptor> {
        val annotations = when (proto) {
            is ProtoBuf.Constructor -> proto.getExtension(protocol.constructorAnnotation)
            is ProtoBuf.Function -> proto.getExtension(protocol.functionAnnotation)
            is ProtoBuf.Property -> proto.getExtension(protocol.propertyAnnotation)
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { annotationProto ->
            deserializer.deserializeAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadPropertyBackingFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<AnnotationDescriptor> =
        emptyList()

    override fun loadPropertyDelegateFieldAnnotations(container: ProtoContainer, proto: ProtoBuf.Property): List<AnnotationDescriptor> =
        emptyList()

    override fun loadEnumEntryAnnotations(container: ProtoContainer, proto: ProtoBuf.EnumEntry): List<AnnotationDescriptor> {
        val annotations = proto.getExtension(protocol.enumEntryAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            deserializer.deserializeAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<AnnotationDescriptor> {
        val annotations = proto.getExtension(protocol.parameterAnnotation).orEmpty()
        return annotations.map { annotationProto ->
            deserializer.deserializeAnnotation(annotationProto, container.nameResolver)
        }
    }

    override fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<AnnotationDescriptor> = emptyList()

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<AnnotationDescriptor> {
        return proto.getExtension(protocol.typeAnnotation).orEmpty().map { deserializer.deserializeAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<AnnotationDescriptor> {
        return proto.getExtension(protocol.typeParameterAnnotation).orEmpty().map { deserializer.deserializeAnnotation(it, nameResolver) }
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: KotlinType): ConstantValue<*>? {
        val value = proto.getExtensionOrNull(protocol.compileTimeValue) ?: return null
        return deserializer.resolveValue(expectedType, value, container.nameResolver)
    }
}
