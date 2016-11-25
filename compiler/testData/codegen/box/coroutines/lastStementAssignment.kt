// IGNORE_BACKEND: JS
class Controller {
    var wasHandleResultCalled = false
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
    }

    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        wasHandleResultCalled = true
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)

    if (!controller.wasHandleResultCalled) throw RuntimeException("fail 1")
}

var varWithCustomSetter: String = ""
    set(value) {
        if (field != "") throw RuntimeException("fail 2")
        field = value
    }

fun box(): String {
    var result = ""

    builder {
        result += "O"

        if (suspendHere() != "OK") throw RuntimeException("fail 3")

        result += "K"
    }

    if (result != "OK") return "fail 4"

    builder {
        if (suspendHere() != "OK") throw RuntimeException("fail 5")

        varWithCustomSetter = "OK"
    }

    return varWithCustomSetter
}
