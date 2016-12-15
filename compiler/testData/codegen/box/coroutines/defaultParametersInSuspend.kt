// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(a: String = "abc", i: Int = 2): String = suspendWithCurrentContinuation { x ->
        x.resume(a + "#" + (i + 1))
        SUSPENDED
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = "OK"

    builder {
        var a = suspendHere()
        if (a != "abc#3") {
            result = "fail 1: $a"
            throw RuntimeException(result)
        }

        a = suspendHere("cde")
        if (a != "cde#3") {
            result = "fail 2: $a"
            throw RuntimeException(result)
        }

        a = suspendHere(i = 6)
        if (a != "abc#7") {
            result = "fail 3: $a"
            throw RuntimeException(result)
        }

        a = suspendHere("xyz", 9)
        if (a != "xyz#10") {
            result = "fail 4: $a"
            throw RuntimeException(result)
        }
    }

    return result
}
