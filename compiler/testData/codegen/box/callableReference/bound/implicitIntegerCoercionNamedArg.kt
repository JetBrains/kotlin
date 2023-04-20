// ISSUE: KT-57655
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// DUMP_IR

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

import kotlin.internal.ImplicitIntegerCoercion

fun test(@ImplicitIntegerCoercion x: UInt) = x as UInt

fun box(): String = when {
    test(5) != 5.toUInt() -> "Fail: test(5)"
    test(x = 5) != 5.toUInt() -> "Fail: test(x = 5)"
    else -> "OK"
}
