// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resumeWithException(RuntimeException("OK"))
        COROUTINE_SUSPENDED
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
