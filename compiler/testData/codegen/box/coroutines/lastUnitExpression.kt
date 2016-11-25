// IGNORE_BACKEND: JS
class Controller {
    var ok = false
    var v  = "fail"
    suspend fun suspendHere(v: String): Unit = suspendWithCurrentContinuation { x ->
        this.v = v
        x.resume(Unit)
    }

    operator fun handleResult(u: Unit, v: Continuation<Nothing>) {
        ok = true
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.ok) throw RuntimeException("Fail 1")
    return controller.v
}

fun box(): String {

    return builder {
        suspendHere("OK")
    }
}
