/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotationsWithPossibleTargets
import org.jetbrains.kotlin.types.AbstractLazyType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.withAbbreviation
import org.jetbrains.kotlin.utils.toReadOnlyList

class DeserializedType private constructor(
        private val c: DeserializationContext,
        private val typeProto: ProtoBuf.Type,
        private val additionalAnnotations: Annotations = Annotations.EMPTY
) : AbstractLazyType(c.storageManager) {
    override fun computeTypeConstructor() = c.typeDeserializer.typeConstructor(typeProto)

    override fun computeArguments() =
            typeProto.collectAllArguments().mapIndexed {
                index, proto ->
                c.typeDeserializer.typeArgument(constructor.parameters.getOrNull(index), proto)
            }.toReadOnlyList()

    private fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

    override val annotations = DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
        c.components.annotationAndConstantLoader
                .loadTypeAnnotations(typeProto, c.nameResolver)
                .map { AnnotationWithTarget(it, null) } + additionalAnnotations.getAllAnnotations()
    }

    override val isMarkedNullable: Boolean get() = typeProto.nullable

    companion object {
        fun create(
                c: DeserializationContext,
                typeProto: ProtoBuf.Type,
                additionalAnnotations: Annotations
        ): SimpleType {
            val deserializedType = DeserializedType(c, typeProto, additionalAnnotations)
            val abbreviatedTypeProto = typeProto.abbreviatedType(c.typeTable) ?: return deserializedType

            return deserializedType.withAbbreviation(DeserializedType(c, abbreviatedTypeProto, additionalAnnotations))
        }
    }
}


