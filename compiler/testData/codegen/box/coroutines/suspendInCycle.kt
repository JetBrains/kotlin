// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    var i = 0
    suspend fun suspendHere(): Int = suspendCoroutineOrReturn { x ->
        x.resume(i++)
        COROUTINE_SUSPENDED
    }
    suspend fun suspendThere(): String = suspendCoroutineOrReturn { x ->
        x.resume("?")
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
        for (i in 0..5) {
            if (i % 2 == 0) {
                result += suspendHere().toString()
            }
            else if (i == 3) {
                result += suspendThere()
            }
        }
        result += "+"
    }

    if (result != "-01?2+") return "fail: $result"

    return "OK"
}
