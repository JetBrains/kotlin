// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun callLocal(): String {
    suspend fun String.local() = suspendCoroutineUninterceptedOrReturn<String> {
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
