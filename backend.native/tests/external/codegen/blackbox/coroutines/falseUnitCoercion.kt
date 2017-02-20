// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun <T> suspendHere(v: T): T = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume(v)
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

var result: Any = ""

fun <T : Any> foo(v: T) {
    builder {
        val r = suspendHere(v)
        suspendHere("")
        result = r
    }
}

fun box(): String {
    foo("OK")
    return result as String
}
