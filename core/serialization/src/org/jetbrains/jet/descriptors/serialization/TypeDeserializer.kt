/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.utils.*

import java.util.LinkedHashMap

public class TypeDeserializer(
        private val context: DeserializationContext,
        private val parent: TypeDeserializer?,
        private val debugName: String
) {
    private val typeParameterDescriptors = LinkedHashMap<Int, TypeParameterDescriptor>()

    private val classDescriptors: (Int) -> ClassDescriptor? = context.components.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeClassDescriptor(fqNameIndex)
    }

    fun addTypeParameter(typeParameterDescriptor: DeserializedTypeParameterDescriptor) {
        typeParameterDescriptors.put(typeParameterDescriptor.getProtoId(), typeParameterDescriptor)
    }

    fun getOwnTypeParameters(): List<TypeParameterDescriptor> = typeParameterDescriptors.values().toReadOnlyList()

    fun type(proto: ProtoBuf.Type): JetType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = context.nameResolver.getString(proto.getFlexibleTypeCapabilitiesId())
            val capabilities = context.components.flexibleTypeCapabilitiesDeserializer.capabilitiesById(id)

            if (capabilities == null) {
                return ErrorUtils.createErrorType("${DeserializedType(context, proto)}: Capabilities not found for id $id")
            }

            return DelegatingFlexibleType.create(
                    DeserializedType(context, proto),
                    DeserializedType(context, proto.getFlexibleUpperBound()),
                    capabilities
            )
        }

        return DeserializedType(context, proto)
    }

    fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        val constructorProto = proto.getConstructor()
        val id = constructorProto.getId()
        return typeConstructor(constructorProto) ?: ErrorUtils.createErrorType(
                if (constructorProto.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS)
                    context.nameResolver.getClassId(id).asSingleFqName().asString()
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
            typeParameterDescriptors.get(proto.getId())?.getTypeConstructor() ?:
            parent?.typeParameterTypeConstructor(proto)

    private fun computeClassDescriptor(fqNameIndex: Int): ClassDescriptor? =
            context.components.moduleDescriptor.findClassAcrossModuleDependencies(context.nameResolver.getClassId(fqNameIndex))

    fun typeArguments(protos: List<ProtoBuf.Type.Argument>): List<TypeProjection> =
            protos.map { proto ->
                TypeProjectionImpl(variance(proto.getProjection()), type(proto.getType()))
            }.toReadOnlyList()

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
