// IGNORE_BACKEND: JS
// WITH_RUNTIME
// NO_INTERCEPT_RESUME_TESTS

// Does not work in JVM backend, probably due to bug. It's not clear which behaviour is right.
// TODO: fix the bug and enable for JVM backend
// TARGET_BACKEND: JS

class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendWithCurrentContinuation { c ->
        result += "suspend($value);"
        c.resume(value)
        Suspend
    }

    operator fun handleResult(value: String, c: Continuation<Nothing>) {
        result += "return($value);"
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.result
}

fun <T> id(value: T) = value

fun box(): String {
    val value = builder {
        try {
            if (id(23) == 23) {
                return@builder suspendAndLog("OK")
            }
        }
        finally {
            result += "finally;"
        }
        result += "afterFinally;"
        "shouldNotReach"
    }
    if (value != "suspend(OK);finally;return(OK);") return "fail: $value"

    return "OK"
}
