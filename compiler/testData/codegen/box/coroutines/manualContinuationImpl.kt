// IGNORE_BACKEND: JS
// WITH_RUNTIME
class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = "fail"

    val lambda: Controller.() -> Continuation<Unit> = l1@{
        object : Continuation<Any?> {
            override fun resume(data: Any?) {
                if (data == Unit) {
                    this@l1.javaClass.getMethod("suspendHere", Continuation::class.java).invoke(this@l1, this)
                    return
                }

                if (data != "OK") {
                    throw RuntimeException("fail: $data")
                }

                result = "OK"
            }

            override fun resumeWithException(exception: Throwable) = throw exception
        }
    }

    builder(lambda)

    return result
}
