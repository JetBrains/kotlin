// WITH_RUNTIME
// WITH_REFLECT

class Controller {
    suspend fun runInstanceOf(x: Continuation<Boolean>) {
        val y: Any = x
        x.resume(x is Continuation<*>)
    }

    suspend fun runCast(x: Continuation<Boolean>) {
        val y: Any = x
        x.resume(Continuation::class.isInstance(y as Continuation<*>))
    }
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
