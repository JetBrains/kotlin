// WITH_RUNTIME
// WITH_COROUTINES
// NO_INTERCEPT_RESUME_TESTS

class Controller {
    var log = ""
    var resumeIndex = 0

    suspend fun <T> suspendWithValue(value: T): T = suspendWithCurrentContinuation { continuation ->
        log += "suspend($value);"
        continuation.resume(value)
        SUSPENDED
    }

    suspend fun suspendWithException(value: String): Unit = suspendWithCurrentContinuation { continuation ->
        log += "error($value);"
        continuation.resumeWithException(RuntimeException(value))
        SUSPENDED
    }
}

fun test(c: @Suspend() (Controller.() -> Unit)): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation, object: ResumeInterceptor {
        private fun interceptResume(block: () -> Unit) {
            val id = controller.resumeIndex++
            controller.log += "before $id;"
            block()
            controller.log += "after $id;"
        }

        override fun <P> interceptResume(data: P, continuation: Continuation<P>): Boolean {
            interceptResume {
                continuation.resume(data)
            }
            return true
        }

        override fun interceptResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean {
            interceptResume {
                continuation.resumeWithException(exception)
            }
            return true
        }
    })
    return controller.log
}

fun box(): String {
    var result = test {
        val o = suspendWithValue("O")
        val k = suspendWithValue("K")
        log += "$o$k;"
    }
    if (result != "before 0;suspend(O);before 1;suspend(K);before 2;OK;after 2;after 1;after 0;") return "fail1: $result"

    result = test {
        try {
            suspendWithException("OK")
            log += "ignore;"
        }
        catch (e: RuntimeException) {
            log += "${e.message};"
        }
    }
    if (result != "before 0;error(OK);before 1;OK;after 1;after 0;") return "fail2: $result"

    return "OK"
}
