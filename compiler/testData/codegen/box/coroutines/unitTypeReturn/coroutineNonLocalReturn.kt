// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    var wasResumeCalled = false
    c.startCoroutine(object : ContinuationAdapter<Unit>() {
        override val context = EmptyCoroutineContext

        override fun resume(value: Unit) {
            wasResumeCalled = true
        }

        override fun resumeWithException(exception: Throwable) {

        }
    })

    if (!wasResumeCalled) throw RuntimeException("fail 1")
}

fun box(): String {
    var result = ""

    builder {
        run {
            if (result == "") return@builder
        }
        suspendHere()
        throw RuntimeException("fail 2")
    }

    result = "fail1"

    builder {
        run {
            if (result == "") return@builder
        }
        result = suspendHere()
    }

    return result
}
