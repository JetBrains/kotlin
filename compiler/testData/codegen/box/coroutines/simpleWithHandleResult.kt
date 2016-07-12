class Controller {
    var res = 0
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    operator fun handleResult(x: Int, y: Continuation<Nothing>) {
        res = x
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): Int {
    val controller = Controller()
    c(controller).resume(Unit)

    return controller.res
}

fun box(): String {
    var result = ""

    val handledResult = builder {
        result = suspendHere()
        56
    }

    if (handledResult != 56) return "fail 1: $handledResult"

    return result
}
