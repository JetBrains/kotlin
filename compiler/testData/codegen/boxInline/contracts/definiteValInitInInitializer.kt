// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING

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
