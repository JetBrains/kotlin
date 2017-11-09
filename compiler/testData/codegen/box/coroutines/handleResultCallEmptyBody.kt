// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


fun builder(c: suspend () -> Unit): String {
    var ok = false
    c.startCoroutine(handleResultContinuation {
        ok = true
    })
    if (!ok) throw RuntimeException("Was not called")
    return "OK"
}

fun unitFun() {}

fun box(): String {
    return builder {}
}
