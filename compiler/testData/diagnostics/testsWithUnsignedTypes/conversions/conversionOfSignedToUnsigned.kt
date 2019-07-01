// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

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

fun takeUIntWithoutAnnotaion(u: UInt) {}

fun takeIntWithoutAnnotation(i: Int) {}

fun test() {
    takeUByte(IMPLICIT_INT)
    takeUByte(EXPLICIT_INT)

    takeUShort(IMPLICIT_INT)
    takeUShort(BIGGER_THAN_UBYTE)

    takeUInt(IMPLICIT_INT)

    takeULong(IMPLICIT_INT)

    takeUBytes(IMPLICIT_INT, EXPLICIT_INT, 42u)

    takeLong(<!NI;TYPE_MISMATCH!>IMPLICIT_INT<!>)

    takeIntWithoutAnnotation(IMPLICIT_INT)

    takeUIntWithoutAnnotaion(UINT_CONST)

    takeUByte(<!TYPE_MISMATCH!>LONG_CONST<!>)
    takeUByte(<!TYPE_MISMATCH!>NON_CONST<!>)
    takeUByte(<!NI;TYPE_MISMATCH, TYPE_MISMATCH!>BIGGER_THAN_UBYTE<!>)
    takeUByte(<!TYPE_MISMATCH!>UINT_CONST<!>)
    takeUIntWithoutAnnotaion(<!TYPE_MISMATCH!>IMPLICIT_INT<!>)
}