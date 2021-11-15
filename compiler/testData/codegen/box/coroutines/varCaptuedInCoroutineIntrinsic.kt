// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): String {
    var s: String? = null
    val z: String = suspendCoroutineUninterceptedOrReturn { x ->
        s = "zzz"
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
    if (s != "zzz") return "fail"
    return z
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
