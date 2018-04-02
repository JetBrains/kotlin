// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun callLocal(): String {
    suspend fun String.local() = suspendCoroutineOrReturn<String> {
        it.resume(this)
        COROUTINE_SUSPENDED
    }
    return "OK".local()
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
