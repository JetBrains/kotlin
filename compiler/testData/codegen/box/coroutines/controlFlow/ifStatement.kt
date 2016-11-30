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
        if (suspendWithResult(true)) {
            result = "OK"
        }
    }
    if (value != "OK") return "fail: suspend as if condition: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            else {
                result += "K"
            }
        }
    }
    if (value != "OK") return "fail: suspend in then branch: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += "O"
            }
            else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in else branch: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            else {
                result += suspendWithResult("K")
            }
        }
    }
    if (value != "OK") return "fail: suspend in both branches: $value"

    value = builder {
        for (x in listOf(true, false)) {
            if (x) {
                result += suspendWithResult("O")
            }
            result += ";"
        }
    }
    if (value != "O;;") return "fail: suspend in then branch without else: $value"

    return "OK"
}
