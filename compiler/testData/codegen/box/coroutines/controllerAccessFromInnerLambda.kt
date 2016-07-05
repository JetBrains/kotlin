class Controller {
    var result = false
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    fun foo() {
        result = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.result) throw java.lang.RuntimeException("fail")
}

fun noinlineRun(block: () -> Unit) {
    block()
}

fun box(): String {
    builder {
        if (suspendHere() != "OK") {
            throw java.lang.RuntimeException("fail 1")
        }
        noinlineRun {
            foo()
        }

        if (suspendHere() != "OK") {
            throw java.lang.RuntimeException("fail 2")
        }
    }

    return "OK"
}
