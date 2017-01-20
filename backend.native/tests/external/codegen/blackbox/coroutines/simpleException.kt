// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resumeWithException(RuntimeException("OK"))
        CoroutineIntrinsics.SUSPENDED
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
