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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection

class TypeAliasExpansion private constructor(
        val parent: TypeAliasExpansion?,
        val descriptor: TypeAliasDescriptor,
        val arguments: List<TypeProjection>,
        val mapping: Map<TypeParameterDescriptor, TypeProjection>
) {
    fun getReplacement(constructor: TypeConstructor): TypeProjection? {
        val descriptor = constructor.declarationDescriptor
        return if (descriptor is TypeParameterDescriptor)
            mapping[descriptor]
        else
            null
    }

    fun isRecursion(descriptor: TypeAliasDescriptor): Boolean =
            this.descriptor == descriptor || (parent?.isRecursion(descriptor) ?: false)

    companion object {
        fun create(
                parent: TypeAliasExpansion?,
                typeAliasDescriptor: TypeAliasDescriptor,
                arguments: List<TypeProjection>
        ): TypeAliasExpansion {
            val typeParameters = typeAliasDescriptor.typeConstructor.parameters.map { it.original }
            val mappedArguments = typeParameters.zip(arguments).toMap()
            return TypeAliasExpansion(parent, typeAliasDescriptor, arguments, mappedArguments)
        }

        fun createWithFormalArguments(typeAliasDescriptor: TypeAliasDescriptor) =
                create(null, typeAliasDescriptor, emptyList())
    }
}
