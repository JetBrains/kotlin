// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(v: Int): Int = suspendWithCurrentContinuation { x ->
        x.resume(v * 2)
        SUSPENDED
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        x(i)
    }
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        foo {
            result += suspendHere(it).toString()
        }
        result += "+"
    }

    if (result != "-24+") return "fail: $result"

    return "OK"
}
