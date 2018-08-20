// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        COROUTINE_SUSPENDED
    }

    // Tail calls are not allowed to be Nothing typed. See KT-15051
    suspend fun suspendLogAndThrow(exception: Throwable): Any? = suspendCoroutineUninterceptedOrReturn { c ->
        result += "throw(${exception.message});"
        c.resumeWithException(exception)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, object : ContinuationAdapter<Unit>() {
        override val context = EmptyCoroutineContext

        override fun resume(data: Unit) {

        }

        override fun resumeWithException(exception: Throwable) {
            controller.result += "caught(${exception.message});"
        }
    })

    return controller.result
}

fun box(): String {
    val value = builder {
        try {
            try {
                suspendAndLog("1")
                suspendLogAndThrow(RuntimeException("exception"))
            }
            catch (e: RuntimeException) {
                suspendAndLog("caught")
                suspendLogAndThrow(RuntimeException("fromCatch"))
            }
        } finally {
            suspendAndLog("finally")
        }
        suspendAndLog("ignore")
    }
    if (value != "suspend(1);throw(exception);suspend(caught);throw(fromCatch);suspend(finally);caught(fromCatch);") {
        return "fail: $value"
    }

    return "OK"
}
