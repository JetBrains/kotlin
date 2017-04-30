// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = throw RuntimeException("OK")
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = try { suspendHere() } catch (e: RuntimeException) { e.message!! }
    }

    return result
}
