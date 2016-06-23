class Controller {
    suspend fun suspendHere(v: String, x: Continuation<String>) {
        x.resume(v)
    }
}

fun builder(coroutine c: Controller.(String, Int) -> Continuation<Unit>) {
    c(Controller(), "OK", 56).resume(Unit)
}

fun noinline(l: () -> String) = l()
inline fun inline(l: () -> String) = l()

fun box(): String {
    var result = ""

    builder { s, i ->
        result = suspendHere(s + "#" + i)
    }

    if (result != "OK#56") return "fail 1: $result"

    builder { s, i ->
        result = suspendHere(noinline {
            s + "#" + i
        })
    }

    if (result != "OK#56") return "fail 2: $result"

    builder { s, i ->
        result = suspendHere(inline {
            s + "#" + i
        })
    }

    if (result != "OK#56") return "fail 3: $result"

    return "OK"
}
