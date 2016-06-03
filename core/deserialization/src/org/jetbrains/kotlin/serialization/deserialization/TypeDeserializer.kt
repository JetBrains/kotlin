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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

class TypeDeserializer(
        private val c: DeserializationContext,
        private val parent: TypeDeserializer?,
        private val typeParameterProtos: List<ProtoBuf.TypeParameter>,
        private val debugName: String
) {
    private val classDescriptors: (Int) -> ClassDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeClassDescriptor(fqNameIndex)
    }

    private val typeAliasDescriptors: (Int) -> ClassifierDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeTypeAliasDescriptor(fqNameIndex)
    }

    private val typeParameterDescriptors = c.storageManager.createLazyValue {
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, TypeParameterDescriptor>()
        }
        else {
            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.id] = DeserializedTypeParameterDescriptor(c, proto, index)
            }
            result
        }
    }

    val ownTypeParameters: List<TypeParameterDescriptor>
            get() = typeParameterDescriptors().values.toReadOnlyList()

    // TODO: don't load identical types from TypeTable more than once
    fun type(proto: ProtoBuf.Type, additionalAnnotations: Annotations = Annotations.EMPTY): KotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = DeserializedType.create(c, proto, additionalAnnotations)
            val upperBound = DeserializedType.create(c, proto.flexibleUpperBound(c.typeTable)!!, additionalAnnotations)
            return c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return DeserializedType.create(c, proto, additionalAnnotations)
    }

    fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor =
            when {
                proto.hasClassName() -> {
                    classDescriptors(proto.className)?.typeConstructor
                    ?: c.components.notFoundClasses.get(proto, c.nameResolver, c.typeTable)
                }
                proto.hasTypeParameter() ->
                    typeParameterTypeConstructor(proto.typeParameter)
                    ?: ErrorUtils.createErrorType("Unknown type parameter ${proto.typeParameter}").constructor
                proto.hasTypeParameterName() -> {
                    val container = c.containingDeclaration
                    val typeParameters = when (container) {
                        is ClassifierDescriptorWithTypeParameters -> container.typeConstructor.parameters
                        is CallableDescriptor -> container.typeParameters
                        else -> emptyList<TypeParameterDescriptor>()
                    }
                    val name = c.nameResolver.getString(proto.typeParameterName)
                    val parameter = typeParameters.find { it.name.asString() == name }
                    parameter?.typeConstructor ?: ErrorUtils.createErrorType("Deserialized type parameter $name in $container").constructor
                }
                proto.hasTypeAliasName() -> {
                    typeAliasDescriptors(proto.typeAliasName)?.typeConstructor
                    ?: TODO("not found type aliases")
                }
                else -> ErrorUtils.createErrorType("Unknown type").constructor
            }

    private fun typeParameterTypeConstructor(typeParameterId: Int): TypeConstructor? =
            typeParameterDescriptors().get(typeParameterId)?.typeConstructor ?:
            parent?.typeParameterTypeConstructor(typeParameterId)

    private fun computeClassDescriptor(fqNameIndex: Int): ClassDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        if (id.isLocal) {
            // Local classes can't be found in scopes
            return c.components.localClassifierResolver.resolveLocalClass(id)
        }
        return c.components.moduleDescriptor.findClassAcrossModuleDependencies(id)
    }

    private fun computeTypeAliasDescriptor(fqNameIndex: Int): ClassifierDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        return if (id.isLocal) {
            c.components.localClassifierResolver.resolveLocalTypeAlias(id)
        }
        else {
            c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
        }
    }

    fun typeArgument(parameter: TypeParameterDescriptor?, typeArgumentProto: ProtoBuf.Type.Argument): TypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return if (parameter == null)
                TypeBasedStarProjectionImpl(c.components.moduleDescriptor.builtIns.nullableAnyType)
            else
                StarProjectionImpl(parameter)
        }

        val variance = Deserialization.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(c.typeTable) ?:
                return TypeProjectionImpl(ErrorUtils.createErrorType("No type recorded"))

        return TypeProjectionImpl(variance, type(type))
    }

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
