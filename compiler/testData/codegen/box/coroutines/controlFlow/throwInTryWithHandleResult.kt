// WITH_RUNTIME

class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T, c: Continuation<T>) {
        result += "suspend($value);"
        c.resume(value)
    }

    operator fun handleException(exception: Throwable, c: Continuation<Nothing>) {
        result += "ignoreCaught(${exception.message});"
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
            suspendAndLog("before")
            throw RuntimeException("foo")
        } catch (e: RuntimeException) {
            result += "caught(${e.message});"
        }
        suspendAndLog("after")
    }
    if (value != "suspend(before);caught(foo);suspend(after);") {
        return "fail: $value"
    }

    return "OK"
}