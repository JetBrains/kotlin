// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

fun foo(b: Boolean): String {
    val res: String
    runOnce {
        b
        res = "OK"
    }
    return res
}

fun box(): String {
    return foo(true)
}
