// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

open class A(val v: String) {
    suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
    }

    open suspend fun suspendHere(): String = suspendThere("O") + suspendThere(v)
}

class B(v: String) : A(v) {
    override suspend fun suspendHere(): String = super.suspendHere() + suspendThere("56")
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

    return "OK"
}
