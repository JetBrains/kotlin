// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun suspendHere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
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
