// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE

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

fun takeUBytes(@ImplicitIntegerCoercion vararg u: <!OPT_IN_USAGE!>UByte<!>) {}

fun takeLong(@ImplicitIntegerCoercion l: Long) {}

fun takeUIntWithoutAnnotaion(u: UInt) {}

fun takeIntWithoutAnnotation(i: Int) {}

fun test() {
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>EXPLICIT_INT<!>)

    takeUShort(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)
    takeUShort(<!ARGUMENT_TYPE_MISMATCH!>BIGGER_THAN_UBYTE<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)

    takeULong(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)

    <!OPT_IN_USAGE!>takeUBytes<!>(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>, <!ARGUMENT_TYPE_MISMATCH!>EXPLICIT_INT<!>, 42u)

    takeLong(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)

    takeIntWithoutAnnotation(IMPLICIT_INT)

    takeUIntWithoutAnnotaion(UINT_CONST)

    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>LONG_CONST<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>NON_CONST<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>BIGGER_THAN_UBYTE<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>UINT_CONST<!>)
    takeUIntWithoutAnnotaion(<!ARGUMENT_TYPE_MISMATCH!>IMPLICIT_INT<!>)
}
