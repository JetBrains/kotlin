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
    val value = builder {
        var r = ""

        for (x in listOf("O", "$", "K")) {
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
