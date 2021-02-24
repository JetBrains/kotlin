// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


var x = true
private suspend fun foo(): String  {
    if (x) {
        return suspendCoroutineUninterceptedOrReturn<String> {
            it.resume("OK")
            COROUTINE_SUSPENDED
        }
    }

    return "fail"
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = ""
    builder {
        res = foo()
    }

    return res
}
