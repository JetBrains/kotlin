// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM_IR, NATIVE, JS_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: 1.kt
package test

import kotlin.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun myrun(crossinline block: () -> Unit): Unit {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val l = { block() }
    l()
}

// FILE: 2.kt
import test.*

fun box(): String {
    val x: Long
    myrun {
        x = 42L
    }
    return if (x != 42L) "FAIL" else "OK"
}