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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.LinkedHashMap

public enum class TypeConstructorKind {
    CLASS,
    TYPE_PARAMETER
}

public data class TypeConstructorData(val kind: TypeConstructorKind, val id: Int)

public fun ProtoBuf.Type.getTypeConstructorData(): TypeConstructorData {
    if (hasConstructorClassName()) {
        assert(!hasConstructorTypeParameter(), "constructor_class_name already presents, so constructor_type_parameter should not be here")
        return TypeConstructorData(TypeConstructorKind.CLASS, constructorClassName)
    }
    else if (hasConstructorTypeParameter()) {
        assert(!hasConstructorClassName(), "constructor_type_parameter already presents, so constructor_class_name should not be here")
        return TypeConstructorData(TypeConstructorKind.TYPE_PARAMETER, constructorTypeParameter)
    }
    else {
        return when (constructor.kind) {
            ProtoBuf.Type.Constructor.Kind.CLASS -> TypeConstructorData(TypeConstructorKind.CLASS, constructor.id)
            ProtoBuf.Type.Constructor.Kind.TYPE_PARAMETER -> TypeConstructorData(TypeConstructorKind.TYPE_PARAMETER, constructor.id)
            else -> throw IllegalStateException("Unknown kind ${constructor.kind}")
        }
    }
}

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
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.getId()] = DeserializedTypeParameterDescriptor(c, proto, index)
            }
            result
        }
    }

    val ownTypeParameters: List<TypeParameterDescriptor>
            get() = typeParameterDescriptors().values().toReadOnlyList()

    fun type(proto: ProtoBuf.Type, additionalAnnotations: Annotations = Annotations.EMPTY): JetType {
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

        return DeserializedType(c, proto, additionalAnnotations)
    }

    fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        val typeConstructorData = proto.getTypeConstructorData()
        val id = typeConstructorData.id
        return typeConstructor(typeConstructorData) ?: ErrorUtils.createErrorType(
                if (typeConstructorData.kind == TypeConstructorKind.CLASS)
                    c.nameResolver.getClassId(id).asSingleFqName().asString()
                else
                    "Unknown type parameter $id"
        ).constructor
    }

    private fun typeConstructor(data: TypeConstructorData): TypeConstructor? = when (data.kind) {
        TypeConstructorKind.CLASS -> classDescriptors(data.id)?.typeConstructor
        TypeConstructorKind.TYPE_PARAMETER -> typeParameterTypeConstructor(data.id)
    }

    private fun typeParameterTypeConstructor(typeParameterId: Int): TypeConstructor? =
            typeParameterDescriptors().get(typeParameterId)?.typeConstructor ?:
            parent?.typeParameterTypeConstructor(typeParameterId)

    private fun computeClassDescriptor(fqNameIndex: Int): ClassDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        if (id.isLocal()) {
            // Local classes can't be found in scopes
            return c.components.localClassResolver.resolveLocalClass(id)
        }
        return c.components.moduleDescriptor.findClassAcrossModuleDependencies(id)
    }

    fun typeArgument(parameter: TypeParameterDescriptor?, typeArgumentProto: ProtoBuf.Type.Argument): TypeProjection {
        return if (typeArgumentProto.getProjection() == ProtoBuf.Type.Argument.Projection.STAR)
            if (parameter == null)
                TypeBasedStarProjectionImpl(c.components.moduleDescriptor.builtIns.getNullableAnyType())
            else
                StarProjectionImpl(parameter)
        else TypeProjectionImpl(variance(typeArgumentProto.getProjection()), type(typeArgumentProto.getType()))
    }

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
