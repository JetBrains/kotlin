// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun callLocal(): String {
    val l: suspend () -> String = {
        suspend fun local() = suspendCoroutineOrReturn<String> {
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
