// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun callLocal(a: String, b: String): String {
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
        res = callLocal("O", "K")
    }
    return res
}
