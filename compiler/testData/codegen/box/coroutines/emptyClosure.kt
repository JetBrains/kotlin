// WITH_RUNTIME
// WITH_COROUTINES
var result = 0

class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        result++
        x.resume("OK")
        SUSPENDED
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}


fun builder(c: @Suspend() (Controller.() -> Unit)) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw RuntimeException("fail 1")
        }
    }

    if (result != 3) return "fail 2: $result"

    return "OK"
}
