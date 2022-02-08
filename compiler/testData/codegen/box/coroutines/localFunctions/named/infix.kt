// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun callLocal(): String {
    suspend infix fun String.local(a: String) = suspendCoroutineUninterceptedOrReturn<String> {
        it.resume(this + a)
        COROUTINE_SUSPENDED
    }
    return "O" local "K"
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
