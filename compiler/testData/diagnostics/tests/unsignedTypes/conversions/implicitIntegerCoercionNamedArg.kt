// FIR_IDENTICAL
// ISSUE: KT-57655
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

import kotlin.internal.ImplicitIntegerCoercion

fun test(@ImplicitIntegerCoercion x: UInt) = x

fun main() {
    println(test(x = 5))
    println(test(5))
}
