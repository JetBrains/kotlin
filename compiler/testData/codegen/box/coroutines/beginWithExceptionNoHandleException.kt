// IGNORE_BACKEND: JS
// WITH_RUNTIME
class Controller {
    suspend fun suspendHere(): Any = suspendWithCurrentContinuation { x ->}

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    try {
        val controller = Controller()
        c(controller).resumeWithException(RuntimeException("OK"))
    }
    catch(e: Exception) {
        if (e?.message != "OK") {
            throw RuntimeException("Unexpected result: ${e?.message}")
        }
        return
    }

    throw RuntimeException("Exception must be thrown above")
}

fun box(): String {
    var result = "OK"
    builder {
        suspendHere()
        result = "fail 1"
    }

    builder {
        result = "fail 2"
    }

    return result
}
