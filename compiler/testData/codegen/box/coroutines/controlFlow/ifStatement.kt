// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var data = ""
fun log(s: String) {
    data += s + ";"
}

class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        log("3")
        c.resume(value)
        // TODO what if with exception?
        // TODO resume >1 times
        log("4")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, Continuation(EmptyCoroutineContext) {})
    return controller.result
}

fun box(): String {
    var value = builder {
        log("0")
        if (suspendWithResult(true)) {
            log("1")
            result = "OK"
        }
        log("2")
    }
    log("5")

    if (value != "OK") return "fail: suspend as if condition: $value"
    if (data != "0;3;1;2;4;5;") return "fail: $data"

    return value
}