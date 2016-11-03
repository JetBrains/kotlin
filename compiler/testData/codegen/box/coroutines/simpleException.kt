// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class Controller {
    suspend fun suspendHere(x: Continuation<String>) {
        x.resumeWithException(RuntimeException("OK"))
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        try {
            suspendHere()
            result = "fail"
        } catch (e: RuntimeException) {
            result = "OK"
        }
    }

    return result
}
