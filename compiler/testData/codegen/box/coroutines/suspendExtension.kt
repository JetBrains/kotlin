@AllowSuspendExtensions
class Controller {
    suspend fun String.suspendHere(x: Continuation<String>) {
        x.resume(this)
    }

    inline suspend fun String.inlineSuspendHere(x: Continuation<String>) {
        suspendHere(x)
    }
}

suspend fun Controller.suspendExtension(v: String, x: Continuation<String>) {
    v.suspendHere(x)
}

inline suspend fun Controller.inlineSuspendExtension(v: String, x: Continuation<String>) {
    v.inlineSuspendHere(x)
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        if ("56".suspendHere() != "56") throw java.lang.RuntimeException("fail 1")
        if ("28".inlineSuspendHere() != "28") throw java.lang.RuntimeException("fail 2")

        if (suspendExtension("123") != "123") throw java.lang.RuntimeException("fail 3")
        result = inlineSuspendExtension("OK")
    }

    return result
}
