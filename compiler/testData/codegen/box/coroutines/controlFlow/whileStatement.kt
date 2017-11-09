// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineOrReturn { c ->
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
        while (suspendWithResult(x) <= 3) {
            result += x++
        }
        result += "."
    }
    if (value != "123.") return "fail: suspend as while condition: $value"

    value = builder {
        var x = 1
        while (x <= 3) {
            result += suspendWithResult(x++)
            result += ";"
        }
        result += "."
    }
    if (value != "1;2;3;.") return "fail: suspend in while body: $value"

    return "OK"
}
