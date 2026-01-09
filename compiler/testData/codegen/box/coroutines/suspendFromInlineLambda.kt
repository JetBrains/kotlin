// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        x(i)
    }
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(v: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v * 2)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        foo { result += suspendHere(it).toString() }
        foo(fun(it: Int) { result += suspendHere(it).toString() })
        result += "+"
    }

    if (result != "-2424+") return "fail: $result"

    return "OK"
}
