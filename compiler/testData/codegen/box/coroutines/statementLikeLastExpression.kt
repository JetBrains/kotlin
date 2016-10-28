var globalResult = ""
class Controller {
    suspend fun suspendWithValue(v: String, x: Continuation<String>) {
        x.resume(v)
    }

    operator fun handleResult(x: String, c: Continuation<Nothing>) {
        globalResult = x
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {

    var condition = true

    builder {
        if (condition) {
            suspendWithValue("OK")
        } else {
            suspendWithValue("fail 1")
        }
    }

    return globalResult
}
