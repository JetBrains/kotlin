// IGNORE_BACKEND: JS
class Controller {
    suspend fun suspendHere(v: String): String = suspendWithCurrentContinuation { x ->
        x.resume(v)

        Suspend
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

inline fun run(block: () -> Unit) {
    block()
}

fun box(): String {
    var result = ""
    run {
        builder {
            try {
                result += suspendHere("O")
            } finally {
                result += suspendHere("K")
            }
        }
    }

    return result
}
