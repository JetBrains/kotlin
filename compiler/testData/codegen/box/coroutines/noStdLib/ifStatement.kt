// IGNORE_BACKEND: JVM, JVM_IR
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
        if (suspendWithResult(true)) {
            result = "OK"
        }
    }
    if (value != "OK") return "fail: suspend as if condition: $value"

    value = builder {
        var i = 0
        while (i < 2) {
            val x = i == 0
            ++i
            if (x) {
                result += suspendWithResult("O")
            } else {
                result += "K"
            }
        }
    }
    if (value != "OK") return "fail: suspend in then branch: $value"

    value = builder {
        var i = 0
        while (i < 2) {
            val x = i == 0
            ++i
            if (x) {
                result += "O"
            } else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in else branch: $value"

    value = builder {
        var i = 0
        while (i < 2) {
            val x = i == 0
            ++i
            if (x) {
                result += suspendWithResult("O")
            } else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in both branches: $value"

    value = builder {
        var i = 0
        while (i < 2) {
            val x = i == 0
            ++i
            if (x) {
                result += suspendWithResult("O")
            }
            result += ";"
        }
    }
    if (value != "O;;") return "fail: suspend in then branch without else: $value"

    return "OK"
}
