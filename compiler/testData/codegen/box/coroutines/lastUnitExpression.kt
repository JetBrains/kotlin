class Controller {
    var ok = false
    var v  = "fail"
    suspend fun suspendHere(v: String, x: Continuation<Unit>) {
        this.v = v
        x.resume(Unit)
    }

    operator fun handleResult(u: Unit, v: Continuation<Nothing>) {
        ok = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.ok) throw java.lang.RuntimeException("Fail 1")
    return controller.v
}

fun box(): String {

    return builder {
        suspendHere("OK")
    }
}
