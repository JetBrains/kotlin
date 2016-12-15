// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, object : Continuation<Unit> {
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
