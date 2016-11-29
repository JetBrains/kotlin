// IGNORE_BACKEND: JS
// WITH_RUNTIME
// NO_INTERCEPT_RESUME_TESTS

class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendWithCurrentContinuation { c ->
        c.resume(value)
        Suspend
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.result
}

fun box(): String {
    val value = builder {
        for (x in listOf("O", "K")) {
            result += suspendWithResult(x)
        }
        result += "."
    }
    if (value != "OK.") return "fail: suspend in for body: $value"

    return "OK"
}
