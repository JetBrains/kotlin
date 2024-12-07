// OPT_IN: kotlin.contracts.ExperimentalContracts
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

fun test(s: String) = s

fun box(): String {
    val x: String
    myrun {
        x = try { test("OK") } catch (e: Exception) { test("fail") }
    }
    return x
}
