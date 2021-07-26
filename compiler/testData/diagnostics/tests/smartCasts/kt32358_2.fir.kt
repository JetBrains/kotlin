// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -EXPERIMENTAL_API_USAGE_FUTURE_ERROR -UNUSED_PARAMETER

import kotlin.contracts.*

inline fun <R> callIt(fn: () -> R): R = TODO()

inline fun <R> callItContracted(fn: () -> R): R {
    <!WRONG_INVOCATION_KIND!>contract {
        callsInPlace(fn, InvocationKind.EXACTLY_ONCE)
    }<!>
    TODO()
}

fun smartIt(p1: String?, p2: String?) {
    p1 ?: callIt { return }
    p1<!UNSAFE_CALL!>.<!>length

    p2 ?: callItContracted { return }
    p2.length
}
