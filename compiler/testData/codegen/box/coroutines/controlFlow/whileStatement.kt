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
    var value = builder {
        var x = 1
        while (suspendWithResult(x) <= 3) {
            result += x++
        }
        result += "."
    }
    if (value != "123.") return "fail: suspend as while condition: $value"

    value = builder {
        var x = 1
        while (x <= 3) {
            result += suspendWithResult(x++)
            result += ";"
        }
        result += "."
    }
    if (value != "1;2;3;.") return "fail: suspend in while body: $value"

    return "OK"
}
