// NO_CHECK_LAMBDA_INLINING
// !OPT_IN: kotlin.contracts.ExperimentalContracts
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

fun box(): String {
    val x: Int
    val res = myrun {
        x = 42
        {
            x
        }.let { it() }
    }
    return if (res == 42 && x.inc() == 43) "OK" else "Fail: ${x.inc()}"
}
