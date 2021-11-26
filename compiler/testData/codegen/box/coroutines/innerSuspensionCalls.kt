// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var i = 0
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
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
