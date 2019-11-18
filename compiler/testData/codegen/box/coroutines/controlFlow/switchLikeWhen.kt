// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        result += "["
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
        for (v in listOf("A", "B", "C")) {
            when (v) {
                "A" -> result += "A;"
                "B" -> result += suspendWithResult(v) + "]"
                else -> result += suspendWithResult(v) + "]!"
            }
        }
    }
    if (value != "A;B]C]!") return "fail: suspend as if condition: $value"

    return "OK"
}
