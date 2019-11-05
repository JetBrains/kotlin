// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM_IR, JS_IR, NATIVE
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR

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

class A {
    val z: String
    init {
        myrun {
            z = "OK"
        }
    }
}

fun box(): String {
    return A().z
}
