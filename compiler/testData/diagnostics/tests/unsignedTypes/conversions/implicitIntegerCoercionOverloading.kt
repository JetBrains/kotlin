// ISSUE: KT-57655
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

import kotlin.internal.ImplicitIntegerCoercion

fun test(@ImplicitIntegerCoercion x: UInt) = x
fun test(@ImplicitIntegerCoercion x: ULong) = x
fun testLong(@ImplicitIntegerCoercion x: ULong) = x

fun box(): String = when {
    test(5) != 5.toUInt() -> "Fail: test(5)"
    <!EQUALITY_NOT_APPLICABLE!>test(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>5L<!>) != 5L.toULong()<!> -> "Fail: test(5L)"
    testLong(5) != 5L.toULong() -> "Fail: test(5L)"
    testLong(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>5L<!>) != 5L.toULong() -> "Fail: test(5L)"
    else -> "OK"
}
