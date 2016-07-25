class Controller {
    var wasHandleResultCalled = false
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        wasHandleResultCalled = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)

    if (!controller.wasHandleResultCalled) throw java.lang.RuntimeException("fail 1")
}

fun box(): String {
    var result = 0

    builder {
        result++

        if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 2")

        result--
    }

    if (result != 0) return "fail 3"

    builder {
        --result

        if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 4")

        ++result
    }

    if (result != 0) return "fail 5"

    return "OK"
}
