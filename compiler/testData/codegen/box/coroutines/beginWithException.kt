// WITH_RUNTIME
class Controller {
    var exception: Throwable? = null

    operator fun handleException(t: Throwable, c: Continuation<Nothing>) {
        exception = t
    }

    suspend fun suspendHere(x: Continuation<Any>) {}
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resumeWithException(RuntimeException("OK"))

    if (controller.exception?.message != "OK") {
        throw RuntimeException("Unexpected result: ${controller.exception?.message}")
    }
}

fun box(): String {
    var result = "OK"
    builder {
        suspendHere()
        result = "fail 1"
    }

    builder {
        result = "fail 2"
    }

    return result
}
