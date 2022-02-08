// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        c.resume(value)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    var value = builder {
        var x = 1
        do {
            result += x
        } while (suspendWithResult(x++) < 3)
        result += "."
    }
    if (value != "123.") return "fail: suspend as do..while condition: $value"

    value = builder {
        var x = 1
        do {
            result += suspendWithResult(x)
            result += ";"
        } while (x++ < 3)
        result += "."
    }
    if (value != "1;2;3;.") return "fail: suspend in do..while body: $value"

    return "OK"
}
