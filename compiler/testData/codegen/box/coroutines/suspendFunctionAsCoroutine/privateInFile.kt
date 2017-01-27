// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


var x = true
private suspend fun foo(): String  {
    if (x) {
        return suspendCoroutineOrReturn<String> {
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
