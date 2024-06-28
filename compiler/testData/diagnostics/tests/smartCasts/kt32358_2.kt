// DIAGNOSTICS: -OPT_IN_USAGE_ERROR -OPT_IN_USAGE_FUTURE_ERROR -UNUSED_PARAMETER

import kotlin.contracts.*

inline fun <R> callIt(fn: () -> R): R = TODO()

inline fun <R> callItContracted(fn: () -> R): R {
    contract {
        callsInPlace(fn, InvocationKind.EXACTLY_ONCE)
    }
    TODO()
}

fun smartIt(p1: String?, p2: String?) {
    p1 ?: callIt { return }
    <!DEBUG_INFO_SMARTCAST!>p1<!>.length

    p2 ?: callItContracted { return }
    <!DEBUG_INFO_SMARTCAST!>p2<!>.length
}
