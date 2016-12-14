// WITH_RUNTIME
// WITH_COROUTINES
class Controller {
    var result = false
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        SUSPENDED
    }

    fun foo() {
        result = true
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: @Suspend() (Controller.() -> Unit)) {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    if (!controller.result) throw RuntimeException("fail")
}

fun noinlineRun(block: () -> Unit) {
    block()
}

fun box(): String {
    builder {
        if (suspendHere() != "OK") {
            throw RuntimeException("fail 1")
        }
        noinlineRun {
            foo()
        }

        if (suspendHere() != "OK") {
            throw RuntimeException("fail 2")
        }
    }

    return "OK"
}
