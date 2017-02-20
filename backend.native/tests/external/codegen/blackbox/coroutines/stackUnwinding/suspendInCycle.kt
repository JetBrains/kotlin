// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): Int = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        1
    }
    suspend fun suspendThere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        "?"
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        for (i in 0..10000) {
            if (i % 2 == 0) {
                result += suspendHere().toString()
            }
            else if (i == 3) {
                result += suspendThere()
            }
        }
        result += "+"
    }

    var mustBe = "-"
    for (i in 0..10000) {
        if (i % 2 == 0) {
            mustBe += "1"
        }
        else if (i == 3) {
            mustBe += "?"
        }
    }
    mustBe += "+"

    if (result != mustBe) return "fail: $result/$mustBe"

    return "OK"
}
