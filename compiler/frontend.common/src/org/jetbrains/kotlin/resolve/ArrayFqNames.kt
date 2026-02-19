/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object ArrayFqNames {
    val PRIMITIVE_TYPE_TO_ARRAY: Map<PrimitiveType, Name> = hashMapOf(
        PrimitiveType.BOOLEAN to Name.identifier("booleanArrayOf"),
        PrimitiveType.CHAR to Name.identifier("charArrayOf"),
        PrimitiveType.INT to Name.identifier("intArrayOf"),
        PrimitiveType.BYTE to Name.identifier("byteArrayOf"),
        PrimitiveType.SHORT to Name.identifier("shortArrayOf"),
        PrimitiveType.FLOAT to Name.identifier("floatArrayOf"),
        PrimitiveType.LONG to Name.identifier("longArrayOf"),
        PrimitiveType.DOUBLE to Name.identifier("doubleArrayOf")
    )

    val UNSIGNED_TYPE_TO_ARRAY: Map<FqName, Name> = hashMapOf(
        FqNames.uByteFqName to Name.identifier("ubyteArrayOf"),
        FqNames.uShortFqName to Name.identifier("ushortArrayOf"),
        FqNames.uIntFqName to Name.identifier("uintArrayOf"),
        FqNames.uLongFqName to Name.identifier("ulongArrayOf")
    )

    val ARRAY_OF_FUNCTION: Name = Name.identifier("arrayOf")
    val ARRAY_OF_NULLS_FUNCTION: Name = Name.identifier("arrayOfNulls")

    val EMPTY_ARRAY: Name = Name.identifier("emptyArray")

    val ARRAY_CALL_NAMES: Set<Name> =
        setOf(ARRAY_OF_FUNCTION, EMPTY_ARRAY) + PRIMITIVE_TYPE_TO_ARRAY.values.toSet() + UNSIGNED_TYPE_TO_ARRAY.values.toSet()

    @JvmField
    val ARRAY_CALL_FQ_NAMES: Set<FqName> = ARRAY_CALL_NAMES.map { FqName("kotlin." + it.identifier) }.toSet()
}
