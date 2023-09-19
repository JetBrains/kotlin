// ISSUE: KT-57655
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// DUMP_IR

// IGNORE_BACKEND_K1: ANDROID
// STATUS:
//  This line has been added because of the `AndroidRunner` test.
//  This test runner appends a path-like prefix to all fully-qualified names,
//  but the compiler logic for `ImplicitSignedToUnsignedIntegerConversion`
//  relies on the ability to check exactly `kotlin.internal.ImplicitIntegerCoercion`.

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

import kotlin.internal.ImplicitIntegerCoercion

fun testInt(@ImplicitIntegerCoercion x: UInt) = x as UInt
fun testLong(@ImplicitIntegerCoercion x: ULong) = x as ULong

fun box(): String = when {
    testInt(5) != 5.toUInt() -> "Fail: testInt(5)"
    testInt(x = 5) != 5.toUInt() -> "Fail: testInt(x = 5)"
    testLong(5) != 5L.toULong() -> "Fail: testLong(5L)"
    testLong(x = 5) != 5L.toULong() -> "Fail: testLong(x = 5L)"
    else -> "OK"
}
