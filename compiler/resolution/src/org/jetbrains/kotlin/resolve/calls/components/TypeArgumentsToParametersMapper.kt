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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType

class TypeArgumentsToParametersMapper {

    sealed class TypeArgumentsMapping(val diagnostics: List<KotlinCallDiagnostic>) :
        Iterable<Map.Entry<TypeParameterDescriptor, KotlinType?>> {

        abstract fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument

        object NoExplicitArguments : TypeArgumentsMapping(emptyList()) {
            private val emptyIterator = mapOf<Nothing, Nothing>().iterator()

            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                TypeArgumentPlaceholder

            override fun iterator() = emptyIterator
        }

        class TypeArgumentsMappingImpl(
            diagnostics: List<KotlinCallDiagnostic>,
            private val typeParameterToArgumentMap: Map<TypeParameterDescriptor, TypeArgument>
        ) : TypeArgumentsMapping(diagnostics) {
            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                typeParameterToArgumentMap[typeParameterDescriptor] ?: TypeArgumentPlaceholder

            override fun iterator() = typeParameterToArgumentMap.mapValues { (it.value as? SimpleTypeArgument)?.type }.iterator()
        }
    }

    fun mapTypeArguments(call: KotlinCall, descriptor: CallableDescriptor): TypeArgumentsMapping {
        if (call.typeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }

        if (call.typeArguments.size != descriptor.typeParameters.size) {
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
            )
        } else {
            val typeParameterToArgumentMap = descriptor.typeParameters.zip(call.typeArguments).associate { it }
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
        }
    }

}

