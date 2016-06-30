class Controller {
    suspend fun suspendHere(v: String, x: Continuation<String>) {
        x.resume(v)
    }
}

fun builder(coroutine c: Controller.(Long, String) -> Continuation<Unit>) {
    c(Controller(), 56L, "OK").resume(Unit)
}

fun noinline(l: () -> String) = l()
inline fun inline(l: () -> String) = l()

fun box(): String {
    var result = ""

    builder { l, s ->
        result = suspendHere(s + "#" + l)
    }

    if (result != "OK#56") return "fail 1: $result"

    builder { l, s ->
        result = suspendHere(noinline {
            s + "#" + l
        })
    }

    if (result != "OK#56") return "fail 2: $result"

    builder { l, s ->
        result = suspendHere(inline {
            s + "#" + l
        })
    }

    if (result != "OK#56") return "fail 3: $result"

    return "OK"
}
