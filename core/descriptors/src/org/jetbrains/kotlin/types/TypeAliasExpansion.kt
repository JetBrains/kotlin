/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

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
