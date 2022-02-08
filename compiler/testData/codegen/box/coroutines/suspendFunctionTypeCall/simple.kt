// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo1(c: suspend () -> Unit) = c()
suspend fun foo2(c: suspend String.() -> Int) = "2".c()
suspend fun foo3(c: suspend (String) -> Int) = c("3")

fun box(): String {
    var result = ""

    builder {
        foo1 {
            result = suspendHere("begin#")
        }

        val q2 = foo2 { result += suspendHere(this) + "#"; 1 }
        val q3 = foo3 { result += suspendHere(it); 2 }

        if (q2 != 1) throw RuntimeException("fail q2")
        if (q3 != 2) throw RuntimeException("fail q3")
    }

    if (result != "begin#2#3") return "fail: $result"

    return "OK"
}
