// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun <T> suspendHere(v: T): T = suspendCoroutineOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
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
