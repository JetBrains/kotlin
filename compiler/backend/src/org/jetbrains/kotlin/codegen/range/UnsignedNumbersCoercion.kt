/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.org.objectweb.asm.Type

val StackValue.unsignedType: UnsignedType?
    get() = kotlinType?.let { UnsignedTypes.toUnsignedType(it) }

fun coerceUnsignedToUInt(stackValue: StackValue, uIntKotlinType: KotlinType): StackValue =
    coerceUnsignedToUInt(stackValue, stackValue.kotlinType, uIntKotlinType)

fun coerceUnsignedToUInt(
    stackValue: StackValue,
    valueKotlinType: KotlinType?,
    uIntKotlinType: KotlinType
): StackValue {
    stackValue.kotlinType?.let {
        if (it.isNothing()) return stackValue
    }

    val valueUnsignedType = stackValue.unsignedType
        ?: throw AssertionError("Unsigned type expected: ${stackValue.kotlinType}")

    if (valueUnsignedType == UnsignedType.UINT) return stackValue

    return StackValue.operation(Type.INT_TYPE, uIntKotlinType) { v ->
        stackValue.put(stackValue.type, valueKotlinType, v)
        when (valueUnsignedType) {
            UnsignedType.UBYTE -> {
                v.iconst(0xFF)
                v.and(Type.INT_TYPE)
            }

            UnsignedType.USHORT -> {
                v.iconst(0xFFFF)
                v.and(Type.INT_TYPE)
            }

            UnsignedType.ULONG -> {
                v.cast(Type.LONG_TYPE, Type.INT_TYPE)
            }

            else -> throw AssertionError("Unexpected value type: $valueKotlinType")
        }
    }
}

fun coerceUnsignedToULong(stackValue: StackValue, uLongKotlinType: KotlinType): StackValue =
    coerceUnsignedToULong(stackValue, stackValue.kotlinType, uLongKotlinType)

fun coerceUnsignedToULong(
    stackValue: StackValue,
    valueKotlinType: KotlinType?,
    uLongKotlinType: KotlinType
): StackValue {
    val valueUnsignedType = stackValue.unsignedType
        ?: throw AssertionError("Unsigned type expected: $valueKotlinType")

    if (valueUnsignedType == UnsignedType.ULONG) return stackValue

    return StackValue.operation(Type.LONG_TYPE, uLongKotlinType) { v ->
        stackValue.put(stackValue.type, valueKotlinType, v)
        when (valueUnsignedType) {
            UnsignedType.UBYTE -> {
                v.cast(Type.INT_TYPE, Type.LONG_TYPE)
                v.lconst(0xFF)
                v.and(Type.LONG_TYPE)
            }

            UnsignedType.USHORT -> {
                v.cast(Type.INT_TYPE, Type.LONG_TYPE)
                v.lconst(0xFFFF)
                v.and(Type.LONG_TYPE)
            }

            UnsignedType.UINT -> {
                v.cast(Type.INT_TYPE, Type.LONG_TYPE)
                v.lconst(0xFFFF_FFFFL)
                v.and(Type.LONG_TYPE)
            }

            else -> throw AssertionError("Unexpected value type: $valueKotlinType")
        }
    }
}
