// IGNORE_BACKEND: JS
class Controller {
    var result = ""
    var ok = false
    suspend fun suspendHere(v: String): Unit = suspendWithCurrentContinuation { x ->
        result += v
        x.resume(Unit)
    }

    operator fun handleResult(u: Unit, v: Continuation<Nothing>) {
        ok = true
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    if (!controller.ok) throw RuntimeException("Fail ok")
    return controller.result
}

fun box(): String {
    val r1 = builder {
        for (i in 5..6) {
            suspendHere(i.toString())
        }
    }

    if (r1 != "56") return "fail 1: $r1"

    val r2 = builder {
        var i = 7
        while (i <= 8) {
            suspendHere(i.toString())
            i++
        }
    }

    if (r2 != "78") return "fail 2: $r2"

    val r3 = builder {
        var i = 9
        do {
            suspendHere(i.toString())
            i++
        } while (i <= 10);
    }

    if (r3 != "910") return "fail 3: $r3"

    return "OK"
}
