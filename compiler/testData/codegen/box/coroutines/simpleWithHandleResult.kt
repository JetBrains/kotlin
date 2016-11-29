// IGNORE_BACKEND: JS
class Controller {
    var res = 0
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        Suspend
    }

    operator fun handleResult(x: Int, y: Continuation<Nothing>) {
        res = x
    }

    // INTERCEPT_RESUME_PLACEHOLDER
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
