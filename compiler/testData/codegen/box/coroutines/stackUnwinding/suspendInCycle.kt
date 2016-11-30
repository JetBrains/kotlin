class Controller {
    suspend fun suspendHere(): Int = suspendWithCurrentContinuation { x ->
        1
    }
    suspend fun suspendThere(): String = suspendWithCurrentContinuation { x ->
        "?"
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
        for (i in 0..10000) {
            if (i % 2 == 0) {
                result += suspendHere().toString()
            }
            else if (i == 3) {
                result += suspendThere()
            }
        }
        result += "+"
    }

    var mustBe = "-"
    for (i in 0..10000) {
        if (i % 2 == 0) {
            mustBe += "1"
        }
        else if (i == 3) {
            mustBe += "?"
        }
    }
    mustBe += "+"

    if (result != mustBe) return "fail: $result/$mustBe"

    return "OK"
}
