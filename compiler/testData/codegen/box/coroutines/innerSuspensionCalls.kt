// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    var i = 0
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resume((i++).toString())
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        result += suspendHere()

        if (result == "-0") {
            builder {
                result += "+"
                result += suspendHere()
                result += suspendHere()
                result += "#"
            }

            result += suspendHere()
            result += "&"
        }
    }

    if (result != "-0+01#1&") return "fail: $result"

    return "OK"
}
