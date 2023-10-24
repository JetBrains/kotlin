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
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.KotlinType

class AnnotationAndConstantLoaderImpl(
    module: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    protocol: SerializerExtensionProtocol,
) : AbstractAnnotationLoader<AnnotationDescriptor>(protocol),
    AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>> {
    private val deserializer = AnnotationDeserializer(module, notFoundClasses)

    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor {
        return deserializer.deserializeAnnotation(proto, nameResolver)
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: KotlinType): ConstantValue<*>? {
        val value = proto.getExtensionOrNull(protocol.compileTimeValue) ?: return null
        return deserializer.resolveValue(expectedType, value, container.nameResolver)
    }

    override fun loadAnnotationDefaultValue(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: KotlinType
    ): ConstantValue<*>? {
        // Implement this method to properly support Annotations Instantiation feature
        return null
    }
}
