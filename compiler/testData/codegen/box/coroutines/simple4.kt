// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var cont: Continuation<String>? = null

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        cont = x
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    cont!!.resume("OK")

    return result
}
