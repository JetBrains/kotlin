// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class A(val v: String) {
    suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
    }

    open suspend fun suspendHere(x: String): String = suspendThere(x) + suspendThere(v)
}

class B(v: String) : A(v) {
    override suspend fun suspendHere(x: String): String = super.suspendHere(x) + suspendThere("56")
}

fun builder(c: suspend A.() -> Unit) {
    c.startCoroutine(B("K"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere("O")
    }

    if (result != "OK56") return "fail 1: $result"

    return "OK"
}
