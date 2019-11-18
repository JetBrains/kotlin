// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun callLocal(a: String, b: String): String {
    suspend fun local(a: String, b: String) = suspendCoroutineUninterceptedOrReturn<String> {
        it.resume(a + b)
        COROUTINE_SUSPENDED
    }
    return local(a, b)
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
