// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
    x.resume("OK")
    CoroutineIntrinsics.SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    var wasResumeCalled = false
    c.startCoroutine(object : Continuation<Unit> {
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
        if (result == "") return@builder
        suspendHere()
        throw RuntimeException("fail 2")
    }

    result = "fail"

    builder {
        if (result == "") return@builder
        result = suspendHere()
    }

    return result
}
