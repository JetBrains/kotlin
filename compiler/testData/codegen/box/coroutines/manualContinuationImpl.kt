class Controller {
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = "fail"

    val lambda: Controller.() -> Continuation<Unit> = {
        object : Continuation<Any?> {
            override fun resume(data: Any?) {
                if (data == Unit) {
                    suspendHere(this)
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
