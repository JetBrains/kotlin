// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun callLocal(): String {
    val a = "O"
    val b = "K"
    suspend fun local() = suspendCoroutineUninterceptedOrReturn<String> {
        it.resume(a + b)
        COROUTINE_SUSPENDED
    }
    return local()
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
