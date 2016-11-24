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
        outer@for (x in listOf("O", "K")) {
            result += suspendWithResult(x)
            for (y in listOf("Q", "W")) {
                result += suspendWithResult(y)
                if (y == "W") {
                    break@outer
                }
            }
        }
        result += "."
    }
    if (value != "OQW.") return "fail: break outer loop: $value"

    value = builder {
        for (x in listOf("O", "K")) {
            result += suspendWithResult(x)
            for (y in listOf("Q", "W")) {
                if (y == "W") {
                    break
                }
                result += suspendWithResult(y)
            }
        }
        result += "."
    }
    if (value != "OQKQ.") return "fail: break inner loop: $value"

    return "OK"
}
