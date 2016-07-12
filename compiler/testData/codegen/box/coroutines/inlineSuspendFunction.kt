// WITH_RUNTIME
class Controller {
    fun withValue(v: String, x: Continuation<String>) {
        x.resume(v)
    }

    suspend inline fun suspendInline(v: String, x: Continuation<String>) {
        withValue(v, x)
    }

    suspend inline fun suspendInline(crossinline b: () -> String, x: Continuation<String>) {
        suspendInline(b(), x)
    }

    suspend inline fun <reified T : Any> suspendInline(x: Continuation<String>) {
        suspendInline({ T::class.java.simpleName }, x)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

class OK

fun box(): String {
    var result = ""

    builder {
        result = suspendInline("56")
        if (result != "56") throw RuntimeException("fail 1")

        result = suspendInline { "57" }
        if (result != "57") throw RuntimeException("fail 2")

        result = suspendInline<OK>()
    }

    return result
}
