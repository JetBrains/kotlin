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
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(IMPLICIT_INT)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(EXPLICIT_INT)

    <!INAPPLICABLE_CANDIDATE!>takeUShort<!>(IMPLICIT_INT)
    <!INAPPLICABLE_CANDIDATE!>takeUShort<!>(BIGGER_THAN_UBYTE)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(IMPLICIT_INT)

    <!INAPPLICABLE_CANDIDATE!>takeULong<!>(IMPLICIT_INT)

    <!INAPPLICABLE_CANDIDATE!>takeUBytes<!>(IMPLICIT_INT, EXPLICIT_INT, 42u)

    <!INAPPLICABLE_CANDIDATE!>takeLong<!>(IMPLICIT_INT)

    takeIntWithoutAnnotation(IMPLICIT_INT)

    takeUIntWithoutAnnotaion(UINT_CONST)

    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(LONG_CONST)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(NON_CONST)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(BIGGER_THAN_UBYTE)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(UINT_CONST)
    <!INAPPLICABLE_CANDIDATE!>takeUIntWithoutAnnotaion<!>(IMPLICIT_INT)
}
