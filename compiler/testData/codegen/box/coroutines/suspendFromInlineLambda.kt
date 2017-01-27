// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(v: Int): Int = suspendCoroutineOrReturn { x ->
        x.resume(v * 2)
        COROUTINE_SUSPENDED
    }
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
