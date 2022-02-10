// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE

// FILE: 1.kt

package test

import kotlin.contracts.*

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
