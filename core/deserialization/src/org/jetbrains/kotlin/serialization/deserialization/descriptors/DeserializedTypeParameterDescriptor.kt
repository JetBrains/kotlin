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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.types.JetType
import java.util.*

class DeserializedTypeParameterDescriptor(
        c: DeserializationContext, private val proto: ProtoBuf.TypeParameter, index: Int) : AbstractLazyTypeParameterDescriptor(c.storageManager, c.containingDeclaration, c.nameResolver.getName(proto.name), Deserialization.variance(proto.variance), proto.reified, index, SourceElement.NO_SOURCE) {
    private val typeDeserializer: TypeDeserializer = c.typeDeserializer
    private val typeTable: TypeTable = c.typeTable

    private val annotations = DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
        c.components.annotationAndConstantLoader
                .loadTypeParameterAnnotations(proto, c.nameResolver)
                .map { AnnotationWithTarget(it, null) }
    }

    override fun getAnnotations(): Annotations = annotations

    override fun resolveUpperBounds(): Set<JetType> {
        val upperBounds = proto.upperBounds(typeTable)
        if (upperBounds.isEmpty()) {
            return setOf(this.builtIns.getDefaultBound())
        }
        val result = LinkedHashSet<JetType>(upperBounds.size())
        for (upperBound in upperBounds) {
            result.add(typeDeserializer.type(upperBound, Annotations.EMPTY))
        }
        return result
    }
}
