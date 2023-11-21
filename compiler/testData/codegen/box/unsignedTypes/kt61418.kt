// WITH_STDLIB
// LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

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
