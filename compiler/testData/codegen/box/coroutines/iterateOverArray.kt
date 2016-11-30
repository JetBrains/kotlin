class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        Suspend
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    val a = arrayOfNulls<String>(2) as Array<String>
    a[0] = "O"
    a[1] = "K"
    var result = ""

    builder {
        for (s in a) {
            // 's' variable must be spilled before suspension point
            // And it's important that it should be treated as a String instance because of 'result += s' after
            // But BasicInterpreter just ignores type of argument array for AALOAD opcode (treating it's return type as plain Object),
            // so we should refine the relevant part in our intepreter
            if (suspendHere() != "OK") throw RuntimeException("fail 1")
            result += s
        }
    }

    return result
}
