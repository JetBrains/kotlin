// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(v: String): String = suspendCoroutineOrReturn { x ->
        x.resume(v)

        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

inline fun run(block: () -> Unit) {
    block()
}

fun box(): String {
    var result = ""
    run {
        builder {
            try {
                result += suspendHere("O")
            } finally {
                result += suspendHere("K")
            }
        }
    }

    return result
}
