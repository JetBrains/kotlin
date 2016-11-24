class Controller {
    var result = "fail"
    operator fun handleResult(u: Unit, c: Continuation<Nothing>) {
        result = "OK"
    }

    suspend fun <T> await(t: T): T = suspendWithCurrentContinuation { c ->
        c.resume(t)
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.result
}

var TRUE = true
var FALSE = false
fun box(): String {
    val r1 = builder { await(Unit) }
    if (r1 != "OK") return "fail 1"

    val r2 = builder {
        if (await(1) != 1) throw RuntimeException("fail1")

        if (TRUE) return@builder
    }
    if (r2 != "OK") return "fail 2"

    val r3 = builder {
        if (await(1) != 1) throw RuntimeException("fail2")

        if (FALSE) return@builder
    }
    if (r3 != "OK") return "fail 3"

    val r4 = builder {
        if (await(1) != 1) throw RuntimeException("fail3")

        return@builder
    }
    if (r4 != "OK") return "fail 4"

    return builder { await(1) }
}
