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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PossiblyInnerType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotationsWithPossibleTargets
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList

class DeserializedType(
        private val c: DeserializationContext,
        private val typeProto: ProtoBuf.Type,
        private val additionalAnnotations: Annotations = Annotations.EMPTY
) : AbstractLazyType(c.storageManager), LazyType {
    private val typeDeserializer: TypeDeserializer get() = c.typeDeserializer

    override fun computeTypeConstructor() = typeDeserializer.typeConstructor(typeProto)

    override fun computeArguments() = typeProto.collectAllArguments().deserialize()

    private fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

    private fun List<ProtoBuf.Type.Argument>.deserialize(): List<TypeProjection> =
            mapIndexed {
                index, proto ->
                typeDeserializer.typeArgument(constructor.parameters.getOrNull(index), proto)
            }.toReadOnlyList()

    private val annotations = DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
        c.components.annotationAndConstantLoader
                .loadTypeAnnotations(typeProto, c.nameResolver)
                .map { AnnotationWithTarget(it, null) } + additionalAnnotations.getAllAnnotations()
    }

    override fun isMarkedNullable(): Boolean = typeProto.nullable

    override fun isError(): Boolean {
        val descriptor = constructor.declarationDescriptor
        return descriptor != null && ErrorUtils.isError(descriptor)
    }

    override fun getAnnotations(): Annotations = annotations

    override fun getCapabilities() = typeCapabilities()

    private val typeCapabilities = c.storageManager.createLazyValue { computeCapabilities() }

    private fun computeCapabilities(): TypeCapabilities {
        val capabilities = c.components.typeCapabilitiesLoader.loadCapabilities(typeProto)

        return computePossiblyInnerType()?.let { it: PossiblyInnerType ->
            CompositeTypeCapabilities(
                    SingletonTypeCapabilities(
                            PossiblyInnerTypeCapability::class.java,
                            PossiblyInnerTypeCapabilityImpl(it)),
                    capabilities)
        } ?: capabilities
    }

    private fun computePossiblyInnerType(): PossiblyInnerType? {
        if (!typeProto.hasClassName()) return null

        val outerType = typeProto.outerType(c.typeTable)?.let { DeserializedType(c, it).computePossiblyInnerType() }

        return PossiblyInnerType(
                constructor.declarationDescriptor as ClassDescriptor,
                typeProto.argumentList.deserialize(),
                outerType)
    }

    private class PossiblyInnerTypeCapabilityImpl(override val possiblyInnerType: PossiblyInnerType?) : PossiblyInnerTypeCapability

    fun getPresentableText(): String = typeDeserializer.presentableTextForErrorType(typeProto)
}
