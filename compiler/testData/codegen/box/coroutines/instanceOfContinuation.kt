// WITH_RUNTIME
// WITH_REFLECT

class Controller {
    suspend fun runInstanceOf(): Boolean = suspendWithCurrentContinuation { x ->
        val y: Any = x
        x.resume(x is Continuation<*>)
    }

    suspend fun runCast(): Boolean = suspendWithCurrentContinuation { x ->
        val y: Any = x
        x.resume(Continuation::class.isInstance(y as Continuation<*>))
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result = runInstanceOf().toString() + "," + runCast().toString()
    }

    if (result != "true,true") return "fail: $result"

    return "OK"
}
