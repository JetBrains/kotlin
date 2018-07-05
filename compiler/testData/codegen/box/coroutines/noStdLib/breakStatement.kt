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
    var value = builder {
        var i1 = 0
        outer@while (i1 < 2) {
            val x = if (i1 == 0) "O" else "K"
            i1++
            result += suspendWithResult(x)
            var i2 = 0
            while (i2 < 2) {
                val y = if (i2 == 0) "Q" else "W"
                i2++
                result += suspendWithResult(y)
                if (y == "W") {
                    break@outer
                }
            }
        }
        result += "."
    }
    if (value != "OQW.") return "fail: break outer loop: $value"

    value = builder {
        var i1 = 0
        while (i1 < 2) {
            val x = if (i1 == 0) "O" else "K"
            i1++
            result += suspendWithResult(x)
            var i2 = 0
            while (i2 < 2) {
                val y = if (i2 == 0) "Q" else "W"
                i2++
                if (y == "W") {
                    break
                }
                result += suspendWithResult(y)
            }
        }
        result += "."
    }
    if (value != "OQKQ.") return "fail: break inner loop: $value"

    return "OK"
}
