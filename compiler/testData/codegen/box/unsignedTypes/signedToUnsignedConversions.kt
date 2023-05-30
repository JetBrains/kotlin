// WITH_STDLIB
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// IGNORE_BACKEND_K1: JVM
// FILE: signedToUnsignedConversions_annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: signedToUnsignedConversions_test.kt

import kotlin.internal.ImplicitIntegerCoercion

@ImplicitIntegerCoercion
const val IMPLICIT_INT = 255

@ImplicitIntegerCoercion
const val EXPLICIT_INT: Int = 255

@ImplicitIntegerCoercion
const val LONG_CONST = 255L

@ImplicitIntegerCoercion
val NON_CONST = 255

@ImplicitIntegerCoercion
const val BIGGER_THAN_UBYTE = 256

@ImplicitIntegerCoercion
const val UINT_CONST = 42u

fun takeUByte(@ImplicitIntegerCoercion u: UByte) {}
fun takeUShort(@ImplicitIntegerCoercion u: UShort) {}
fun takeUInt(@ImplicitIntegerCoercion u: UInt) {}
fun takeULong(@ImplicitIntegerCoercion u: ULong) {}

fun takeUBytes(@ImplicitIntegerCoercion vararg u: UByte) {}

fun takeLong(@ImplicitIntegerCoercion l: Long) {}

fun box(): String {
    takeUByte(IMPLICIT_INT)
    takeUByte(EXPLICIT_INT)

    takeUShort(IMPLICIT_INT)
    takeUShort(BIGGER_THAN_UBYTE)

    takeUInt(IMPLICIT_INT)

    takeULong(IMPLICIT_INT)

    takeUBytes(IMPLICIT_INT, EXPLICIT_INT, 42u)

//    such kind of conversions (Int <-> Long) actually are not supported
//    takeLong(IMPLICIT_INT)
    return "OK"
}