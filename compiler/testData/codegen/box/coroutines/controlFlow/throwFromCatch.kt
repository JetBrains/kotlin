// WITH_RUNTIME

class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T, c: Continuation<T>) {
        result += "suspend($value);"
        c.resume(value)
    }

    suspend fun suspendLogAndThrow(exception: Throwable, c: Continuation<Nothing>) {
        result += "throw(${exception.message});"
        c.resumeWithException(exception)
    }

    operator fun handleException(exception: Throwable, c: Continuation<Nothing>) {
        result += "caught(${exception.message});"
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.result
}

fun box(): String {
    val value = builder {
        try {
            try {
                suspendAndLog("1")
                suspendLogAndThrow(RuntimeException("exception"))
            }
            catch (e: RuntimeException) {
                suspendAndLog("caught")
                suspendLogAndThrow(RuntimeException("fromCatch"))
            }
        } finally {
            suspendAndLog("finally")
        }
        suspendAndLog("ignore")
    }
    if (value != "suspend(1);throw(exception);suspend(caught);throw(fromCatch);suspend(finally);caught(fromCatch);") {
        return "fail: $value"
    }

    return "OK"
}