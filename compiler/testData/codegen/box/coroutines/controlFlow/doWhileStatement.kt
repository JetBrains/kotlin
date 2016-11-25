// IGNORE_BACKEND: JS
// WITH_RUNTIME
// NO_INTERCEPT_RESUME_TESTS

class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendWithCurrentContinuation { c ->
        c.resume(value)
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
        do {
            result += x
        } while (suspendWithResult(x++) < 3)
        result += "."
    }
    if (value != "123.") return "fail: suspend as do..while condition: $value"

    value = builder {
        var x = 1
        do {
            result += suspendWithResult(x)
            result += ";"
        } while (x++ < 3)
        result += "."
    }
    if (value != "1;2;3;.") return "fail: suspend in do..while body: $value"

    return "OK"
}
