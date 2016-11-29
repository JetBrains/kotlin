class Controller {
    suspend fun suspendHere(): String = throw RuntimeException("OK")

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result = try { suspendHere() } catch (e: RuntimeException) { e.message!! }
    }

    return result
}
