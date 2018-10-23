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
            is ProtoBuf.Property -> proto.getExtension(protocol.propertyAnnotation)
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
    ) {}
}
