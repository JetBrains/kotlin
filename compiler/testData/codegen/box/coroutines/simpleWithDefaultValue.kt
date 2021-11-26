// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(a: String, b: String = a, c: String = a + b): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("$a:$b:$c")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere("AAA")
    }

    if (result != "AAA:AAA:AAAAAA") return "FAIL 1: $result"

    builder {
        result = suspendHere("AAA", c = "CCC")
    }

    if (result != "AAA:AAA:CCC") return "FAIL 2: $result"

    builder {
        result = suspendHere("AAA", "BBB")
    }

    if (result != "AAA:BBB:AAABBB") return "FAIL 3: $result"

    builder {
        result = suspendHere("AAA", "BBB", "CCC")
    }

    if (result != "AAA:BBB:CCC") return "FAIL 4: $result"

    return "OK"
}
