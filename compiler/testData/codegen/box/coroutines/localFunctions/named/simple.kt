// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

suspend fun callLocal(): String {
    suspend fun local() = "OK"
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
