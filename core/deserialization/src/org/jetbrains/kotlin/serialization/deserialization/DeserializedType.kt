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

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotations
import org.jetbrains.kotlin.types.AbstractLazyType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.LazyType
import org.jetbrains.kotlin.utils.toReadOnlyList

class DeserializedType(
        private val c: DeserializationContext,
        private val typeProto: ProtoBuf.Type
) : AbstractLazyType(c.storageManager), LazyType {
    private val typeDeserializer = c.typeDeserializer

    override fun computeTypeConstructor() = typeDeserializer.typeConstructor(typeProto)

    override fun computeArguments() =
        typeProto.getArgumentList().mapIndexed {
            index, proto ->
            typeDeserializer.typeArgument(getConstructor().getParameters().getOrNull(index), proto)
        }.toReadOnlyList()

    private val annotations = DeserializedAnnotations(c.storageManager) {
        c.components.annotationAndConstantLoader.loadTypeAnnotations(typeProto, c.nameResolver)
    }

    override fun isMarkedNullable(): Boolean = typeProto.getNullable()

    override fun isError(): Boolean {
        val descriptor = getConstructor().getDeclarationDescriptor()
        return descriptor != null && ErrorUtils.isError(descriptor)
    }

    override fun getAnnotations(): Annotations = annotations

    override fun getCapabilities() = c.components.typeCapabilitiesLoader.loadCapabilities(typeProto)

    private fun <E: Any> List<E>.getOrNull(index: Int): E? {
        return if (index in indices) this[index] else null
    }
}
