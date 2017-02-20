// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(v: String): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume(v)

        CoroutineIntrinsics.SUSPENDED
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
