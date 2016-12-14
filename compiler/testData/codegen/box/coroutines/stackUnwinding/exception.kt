// WITH_RUNTIME
// WITH_COROUTINES
class Controller {
    suspend fun suspendHere(): String = throw RuntimeException("OK")

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = try { suspendHere() } catch (e: RuntimeException) { e.message!! }
    }

    return result
}
