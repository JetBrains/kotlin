// !LANGUAGE: -NewInference
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE

import kotlin.contracts.*

public fun myrun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun box(): String {
    val x: Long
    myrun {
        x = 1L
    }
    return if (x != 1L) "FAIL" else "OK"
}
