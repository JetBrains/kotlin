class Controller {
    var ok = false

    operator fun handleResult(u: Unit, v: Continuation<Nothing>) {
        ok = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.ok) throw java.lang.RuntimeException("Was not called")
    return "OK"
}

fun unitFun() {}

fun box(): String {
    return builder {}
}
