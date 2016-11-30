class Controller {
    var i = 0
    suspend fun suspendHere(): Int = suspendWithCurrentContinuation { x ->
        x.resume(i++)
        Suspend
    }
    suspend fun suspendThere(): String = suspendWithCurrentContinuation { x ->
        x.resume("?")
        Suspend
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        for (i in 0..5) {
            if (i % 2 == 0) {
                result += suspendHere().toString()
            }
            else if (i == 3) {
                result += suspendThere()
            }
        }
        result += "+"
    }

    if (result != "-01?2+") return "fail: $result"

    return "OK"
}
