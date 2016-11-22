class Controller {
    suspend fun <T> suspendHere(v: T, x: Continuation<T>) {
        x.resume(v)
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

var result: Any = ""

fun <T : Any> foo(v: T) {
    builder {
        val r = suspendHere(v)
        suspendHere("")
        result = r
    }
}

fun box(): String {
    foo("OK")
    return result as String
}
