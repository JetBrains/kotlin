class Controller {
    suspend fun suspendHere(v: Int, x: Continuation<Int>) {
        x.resume(v * 2)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        x(i)
    }
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        foo {
            result += suspendHere(it).toString()
        }
        result += "+"
    }

    if (result != "-24+") return "fail: $result"

    return "OK"
}
