class Controller {
    var log = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendWithCurrentContinuation { x ->
        log += "suspend($value);"
        x.resume(value)
        Suspend
    }

    operator fun handleResult(value: String, y: Continuation<Nothing>) {
        log += "return($value);"
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.log
}

fun box(): String {
    val result = builder { suspendAndLog("OK") }

    if (result != "suspend(OK);return(OK);") return "fail: $result"

    return "OK"
}
