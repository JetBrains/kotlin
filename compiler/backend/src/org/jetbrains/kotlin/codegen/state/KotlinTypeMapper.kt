/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.org.objectweb.asm.Type

abstract class KotlinTypeMapper {
    companion object {
        @JvmStatic
        fun mapUnderlyingTypeOfInlineClassType(kotlinType: KotlinTypeMarker, typeMapper: KotlinTypeMapperBase): Type {
            val underlyingType = with(typeMapper.typeSystem) {
                kotlinType.getUnsubstitutedUnderlyingTypeInJvm()
            } ?: throw IllegalStateException("There should be underlying type for inline class type: $kotlinType")
            return typeMapper.mapTypeCommon(underlyingType, TypeMappingMode.DEFAULT)
        }

        const val BOX_JVM_METHOD_NAME = "box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS

        const val UNBOX_JVM_METHOD_NAME = "unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS
    }
}
