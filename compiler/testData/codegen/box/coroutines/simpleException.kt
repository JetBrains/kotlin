// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resumeWithException(RuntimeException("OK"))
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        try {
            suspendHere()
            result = "fail"
        } catch (e: RuntimeException) {
            result = "OK"
        }
    }

    return result
}
