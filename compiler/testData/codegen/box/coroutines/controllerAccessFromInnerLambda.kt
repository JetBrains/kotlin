class Controller {
    var result = false
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        Suspend
    }

    fun foo() {
        result = true
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)
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
