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
        if (suspendWithResult(true)) {
            result = "OK"
        }
    }
    if (value != "OK") return "fail: suspend as if condition: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            else {
                result += "K"
            }
        }
    }
    if (value != "OK") return "fail: suspend in then branch: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += "O"
            }
            else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in else branch: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in both branches: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            result += ";"
        }
    }
    if (value != "O;;") return "fail: suspend in then branch without else: $value"

    return "OK"
}
