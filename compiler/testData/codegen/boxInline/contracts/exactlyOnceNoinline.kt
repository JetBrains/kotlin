// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

package test

import kotlin.contracts.*

public inline fun myrun(noinline block: () -> Unit): Unit {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
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
