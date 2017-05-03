// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resume(data: Unit) {}

        override fun resumeWithException(exception: Throwable) {
            controller.result += "ignoreCaught(${exception.message});"
        }
    })
    return controller.result
}

fun box(): String {
    val value = builder {
        try {
            suspendAndLog("before")
            throw RuntimeException("foo")
        } catch (e: RuntimeException) {
            result += "caught(${e.message});"
        }
        suspendAndLog("after")
    }
    if (value != "suspend(before);caught(foo);suspend(after);") {
        return "fail: $value"
    }

    return "OK"
}
