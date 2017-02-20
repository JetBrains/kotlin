// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    var i = 0
    suspend fun suspendHere(): Int = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume(i++)
        CoroutineIntrinsics.SUSPENDED
    }
    suspend fun suspendThere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume("?")
        CoroutineIntrinsics.SUSPENDED
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
