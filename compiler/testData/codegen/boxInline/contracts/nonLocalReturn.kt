// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE

// FILE: 1.kt

package test

import kotlin.contracts.*

public inline fun <R> myrun(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val x: Long
    myrun {
        x = 42L
        if (x == 42L) return "OK"
    }
    return "fail"
}
