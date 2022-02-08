// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

interface A {
    val v: String

    suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
    }

    suspend fun suspendHere(): String = suspendThere("O") + suspendThere(v)
}

interface A2 : A {
    override suspend fun suspendHere(): String = super.suspendHere() + suspendThere("56")
}

class B(override val v: String) : A2

fun builder(c: suspend A.() -> Unit) {
    c.startCoroutine(B("K"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    if (result != "OK56") return "fail 1: $result"

    return "OK"
}
