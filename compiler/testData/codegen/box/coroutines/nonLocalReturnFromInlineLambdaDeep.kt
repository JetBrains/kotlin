// WITH_RUNTIME

class Controller {
    var cResult = 0
    suspend fun suspendHere(v: Int): Int = suspendWithCurrentContinuation { x ->
        x.resume(v * 2)
        Suspend
    }

    operator fun handleResult(x: Int, y: Continuation<Nothing>) {
        cResult = x
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): Controller {
    val controller = Controller()
    c(controller).resume(Unit)

    return controller
}

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        run {
            x(i)
        }
    }
}

fun box(): String {
    var result = ""

    val controllerResult = builder {
        result += "-"
        foo {
            run {
                result += suspendHere(it).toString()
                if (it == 2) return@builder 56
            }
        }
        // Should be unreachable
        result += "+"
        1
    }.cResult

    if (result != "-24") return "fail 1: $result"
    if (controllerResult != 56) return "fail 2: $controllerResult"

    return "OK"
}
