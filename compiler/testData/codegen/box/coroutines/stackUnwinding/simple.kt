class Controller {
    suspend fun suspendHere() = "OK"

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
