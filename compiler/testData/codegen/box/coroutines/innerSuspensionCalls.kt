class Controller {
    var i = 0
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume((i++).toString())
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        result += suspendHere()

        if (result == "-0") {
            builder {
                result += "+"
                result += suspendHere()
                result += suspendHere()
                result += "#"
            }

            result += suspendHere()
            result += "&"
        }
    }

    if (result != "-0+01#1&") return "fail: $result"

    return "OK"
}
