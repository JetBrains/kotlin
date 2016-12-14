// WITH_RUNTIME
// WITH_COROUTINES
class Controller {
    suspend fun suspendHere() = "OK"

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
