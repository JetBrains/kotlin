class Controller {
    var isCompleted = false
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        isCompleted = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.isCompleted) throw java.lang.RuntimeException("fail")
}

fun box(): String {
    builder {
        "OK"
    }

    builder {
        suspendHere()
    }

    return "OK"
}
