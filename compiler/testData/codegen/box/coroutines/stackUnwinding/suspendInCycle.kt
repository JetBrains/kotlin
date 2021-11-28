// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
        1
    }
    suspend fun suspendThere(): String = suspendCoroutineUninterceptedOrReturn { x ->
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
