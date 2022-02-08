// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

fun foo(): String {
    var res = "FAIL"
    try {
        error("OK")
    } catch(e: Exception) {
        runOnce {
            res = e.message!!
        }
    }
    return res
}

fun box(): String {
    return foo()
}
