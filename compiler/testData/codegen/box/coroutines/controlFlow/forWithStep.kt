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
    val value = builder {
        for (x: Long in 20L..30L step 5L) {
            listOf("#").forEach {
                result += it + suspendWithResult(x).toString()
            }
        }
        result += "."
    }
    if (value != "#20#25#30.") return "fail: suspend in for body: $value"

    return "OK"
}
