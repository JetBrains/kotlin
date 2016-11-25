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
    val value = builder {
        try {
            outer@for (x in listOf("A", "B")) {
                try {
                    result += suspendWithResult(x)
                    for (y in listOf("C", "D")) {
                        try {
                            result += suspendWithResult(y)
                            if (y == "D") {
                                break@outer
                            }
                        }
                        finally {
                            result += "!"
                        }
                        result += "E"
                    }
                }
                finally {
                    result += "@"
                }
                result += "ignore"
            }
            result += "*"
        }
        finally {
            result += "finally"
        }
        result += "."
    }
    if (value != "AC!ED!@*finally.") return "fail: $value"

    return "OK"
}
