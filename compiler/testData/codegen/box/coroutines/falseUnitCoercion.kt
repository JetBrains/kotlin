// WITH_RUNTIME
// WITH_COROUTINES
class Controller {
    suspend fun <T> suspendHere(v: T): T = suspendWithCurrentContinuation { x ->
        x.resume(v)
        SUSPENDED
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: @Suspend() (Controller.() -> Unit)) {
    c.startCoroutine(Controller(), EmptyContinuation)
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
