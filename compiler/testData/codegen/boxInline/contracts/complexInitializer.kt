// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
package test

import kotlin.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> myrun(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// FILE: 2.kt

import test.*

fun cond() = true
fun test(s: String) = s

fun box(): String {
    val x: String
    myrun {
        x = if (cond()) test("OK") else test("fail")
    }
    return x
}