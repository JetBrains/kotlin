// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

fun ok(): String {
    val res: String
    when (val o = "OK") {
        else -> runOnce {
            res = o
        }
    }
    return res
}

fun box(): String {
    return ok()
}
