/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType

enum class JsPrimitiveArrayType(
    val kotlinName: String,
    val jsName: String?,
    val isUnsigned: Boolean = false,
    val requiresBigInt: Boolean = false,
) {
    // Signed
    BYTE_ARRAY("ByteArray", "Int8Array"),
    SHORT_ARRAY("ShortArray", "Int16Array"),
    INT_ARRAY("IntArray", "Int32Array"),
    LONG_ARRAY("LongArray", "BigInt64Array", requiresBigInt = true),
    FLOAT_ARRAY("FloatArray", "Float32Array"),
    DOUBLE_ARRAY("DoubleArray", "Float64Array"),

    // Unsigned
    UBYTE_ARRAY("UByteArray", "Uint8Array", isUnsigned = true),
    USHORT_ARRAY("UShortArray", "Uint16Array", isUnsigned = true),
    UINT_ARRAY("UIntArray", "Uint32Array", isUnsigned = true),
    ULONG_ARRAY("ULongArray", "BigUint64Array", isUnsigned = true, requiresBigInt = true),

    // Other
    BOOLEAN_ARRAY("BooleanArray", null),
    CHAR_ARRAY("CharArray", null),
}

internal fun PrimitiveType.toJsPrimitiveArrayType(): JsPrimitiveArrayType =
    when (this) {
        BYTE -> BYTE_ARRAY
        SHORT -> SHORT_ARRAY
        INT -> INT_ARRAY
        LONG -> LONG_ARRAY
        FLOAT -> FLOAT_ARRAY
        DOUBLE -> DOUBLE_ARRAY
        BOOLEAN -> BOOLEAN_ARRAY
        CHAR -> CHAR_ARRAY
    }

internal fun UnsignedType.toJsPrimitiveArrayType(): JsPrimitiveArrayType =
    when (this) {
        UBYTE -> UBYTE_ARRAY
        USHORT -> USHORT_ARRAY
        UINT -> UINT_ARRAY
        ULONG -> ULONG_ARRAY
    }
