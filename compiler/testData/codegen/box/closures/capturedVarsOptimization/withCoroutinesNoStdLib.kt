// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
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
    val value = builder {
        var r = ""

        var _i = 0
        while (_i < 3) {
            val i = _i++
            val x = if (i == 0) "O" else if (i == 1) "$" else "K"
            if (x == "$") continue
            run {
                r += suspendWithResult(x)
            }
        }
        run {
            r += "."
        }
        result = r
    }

    if (value != "OK.") return "fail: suspend in for body: $value"

    return "OK"
}
