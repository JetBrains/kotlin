// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendWithCurrentContinuation { c ->
        c.resume(value)
        SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    val value = builder {
        for (x in listOf("O", "K")) {
            result += suspendWithResult(x)
        }
        result += "."
    }
    if (value != "OK.") return "fail: suspend in for body: $value"

    return "OK"
}
