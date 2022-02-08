// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE, JS
// WITH_STDLIB

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

fun ok(): String {
    val res: String
    val (o, _) = "OK" to "FAIL"
    runOnce {
        res = o
    }
    return res
}

fun box(): String {
    return ok()
}
