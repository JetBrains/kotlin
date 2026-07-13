// WITH_STDLIB
// LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE
// FILE: signedToUnsignedConversions_annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: kt61418_test.kt

import kotlin.internal.ImplicitIntegerCoercion

fun f(@ImplicitIntegerCoercion x: List<Int>) {}

fun box(): String {
    f(listOf(1,2,3))
    return "OK"
}
