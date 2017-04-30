// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendThere()

    suspend fun suspendThere(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
