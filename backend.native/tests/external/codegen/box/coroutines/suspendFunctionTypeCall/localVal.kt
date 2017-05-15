// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(): String {
    var a = "OK"
    var i = 0
    val x: suspend () -> String = {
        suspendHere(a[i++].toString())
    }

    return x() + x.invoke()
}


fun box(): String {
    var result = ""

    builder {
        result = foo()
    }

    return result
}
