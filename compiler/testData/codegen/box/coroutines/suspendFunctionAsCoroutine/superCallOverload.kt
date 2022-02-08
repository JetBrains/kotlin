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

    open suspend fun suspendHere(): String = suspendThere("O") + suspendThere(v)
}

class B(v: String) : A(v) {
    override suspend fun suspendHere(): String = super.suspendHere() + suspendThere("56")
    suspend fun suspendHere(s: String): String = super.suspendHere() + suspendThere(s)
}

fun builder(c: suspend A.() -> Unit) {
    c.startCoroutine(B("K"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    if (result != "OK56") return "fail 1: $result"

    builder {
        result = (this as B).suspendHere("OK")
    }

    if (result != "OKOK") return "fail 2: $result"

    return "OK"
}
