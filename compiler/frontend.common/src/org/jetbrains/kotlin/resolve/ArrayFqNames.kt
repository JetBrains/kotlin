/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.PrimitiveType
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

    val ARRAY_OF_FUNCTION = Name.identifier("arrayOf")
}