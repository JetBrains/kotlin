// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result = 0

    builder {
        result = factorial(4)
    }

    while (postponed != null) {
        postponed!!()
    }

    if (result != 24) return "fail1: $result"
    if (log != "1;1;2;6;24;") return "fail2: $log"

    return "OK"
}

suspend fun factorial(a: Int): Int = if (a > 0) suspendHere(factorial(a - 1) * a) else suspendHere(1)

suspend fun suspendHere(value: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
    postponed = {
        log += "$value;"
        x.resume(value)
    }
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleResultContinuation {
        postponed = null
    })
}

var postponed: (() -> Unit)? = { }

var log = ""
