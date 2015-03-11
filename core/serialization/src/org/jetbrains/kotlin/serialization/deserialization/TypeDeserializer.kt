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
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.LinkedHashMap

public class TypeDeserializer(
        private val c: DeserializationContext,
        private val parent: TypeDeserializer?,
        private val typeParameterProtos: List<ProtoBuf.TypeParameter>,
        private val debugName: String
) {
    private val classDescriptors: (Int) -> ClassDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeClassDescriptor(fqNameIndex)
    }

    private val typeParameterDescriptors = c.storageManager.createLazyValue {
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, TypeParameterDescriptor>()
        }
        else {
            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
            for ((index, proto) in typeParameterProtos.withIndices()) {
                result[proto.getId()] = DeserializedTypeParameterDescriptor(c, proto, index)
            }
            result
        }
    }

    val ownTypeParameters: List<TypeParameterDescriptor>
            get() = typeParameterDescriptors().values().toReadOnlyList()

    fun type(proto: ProtoBuf.Type): JetType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(proto.getFlexibleTypeCapabilitiesId())
            val capabilities = c.components.flexibleTypeCapabilitiesDeserializer.capabilitiesById(id)

            if (capabilities == null) {
                return ErrorUtils.createErrorType("${DeserializedType(c, proto)}: Capabilities not found for id $id")
            }

            return DelegatingFlexibleType.create(
                    DeserializedType(c, proto),
                    DeserializedType(c, proto.getFlexibleUpperBound()),
                    capabilities
            )
        }

        return DeserializedType(c, proto)
    }

    fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        val constructorProto = proto.getConstructor()
        val id = constructorProto.getId()
        return typeConstructor(constructorProto) ?: ErrorUtils.createErrorType(
                if (constructorProto.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS)
                    c.nameResolver.getClassId(id).asSingleFqName().asString()
                else
                    "Unknown type parameter $id"
        ).getConstructor()
    }

    private fun typeConstructor(proto: ProtoBuf.Type.Constructor): TypeConstructor? = when (proto.getKind()) {
        ProtoBuf.Type.Constructor.Kind.CLASS -> classDescriptors(proto.getId())?.getTypeConstructor()
        ProtoBuf.Type.Constructor.Kind.TYPE_PARAMETER -> typeParameterTypeConstructor(proto)
        else -> throw IllegalStateException("Unknown kind ${proto.getKind()}")
    }

    private fun typeParameterTypeConstructor(proto: ProtoBuf.Type.Constructor): TypeConstructor? =
            typeParameterDescriptors().get(proto.getId())?.getTypeConstructor() ?:
            parent?.typeParameterTypeConstructor(proto)

    private fun computeClassDescriptor(fqNameIndex: Int): ClassDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        if (id.isLocal()) {
            // Local classes can't be found in scopes
            return c.components.localClassResolver.resolveLocalClass(id)
        }
        return c.components.moduleDescriptor.findClassAcrossModuleDependencies(id)
    }

    fun typeArguments(protos: List<ProtoBuf.Type.Argument>): List<TypeProjection> =
            protos.map { proto ->
                val type = type(proto.getType())
                if (proto.getProjection() == ProtoBuf.Type.Argument.Projection.STAR)
                    StarProjectionImpl(type)
                else TypeProjectionImpl(variance(proto.getProjection()), type)
            }.toReadOnlyList()

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
