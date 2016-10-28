var result = 0

class Controller {
    suspend fun suspendHere(x: Continuation<String>) {
        result++
        x.resume("OK")
    }
}


fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 1")
        }
    }

    if (result != 3) return "fail 2: $result"

    return "OK"
}
