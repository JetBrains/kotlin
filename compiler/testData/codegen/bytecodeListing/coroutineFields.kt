class Controller {
    suspend fun suspendHere() = suspendWithCurrentContinuation<String> { x ->
        x.resume("OK")
    }
}

fun builder(coroutine c: Controller.(String, Long) -> Continuation<Unit>) {
    c(Controller(), "", 2L).resume(Unit)
}

fun box(): String {
    var result = ""

    builder { x, y ->
        val z = ""
        val u = 1L
        result = suspendHere()

        result += z + u
    }

    return result
}
