// !OPT_IN: kotlin.contracts.ExperimentalContracts
// NO_CHECK_LAMBDA_INLINING
// JVM_ABI_K1_K2_DIFF: KT-62464

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
    val x: String

    constructor() {
    }

    init {
        val o: String
        val k: String = "K"
        myrun { o = "O" }
        fun baz() = o + k
        x = baz()
    }
}

fun box() = A().x
