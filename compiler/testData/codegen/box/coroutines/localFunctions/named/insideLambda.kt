// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun callLocal(): String {
    val l: suspend () -> String = {
        suspend fun local() = suspendCoroutineUninterceptedOrReturn<String> {
            it.resume("OK")
            COROUTINE_SUSPENDED
        }
        local()
    }
    return l()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = callLocal()
    }
    return res
}
