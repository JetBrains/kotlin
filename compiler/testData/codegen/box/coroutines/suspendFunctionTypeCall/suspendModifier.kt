// !API_VERSION: 1.2
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(v: String): String = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun foo(): String {
    var a = "OK"
    var i = 0
    val x = suspend {
        suspendHere(a[i++].toString())
    }

    return x() + x.invoke()
}


fun box(): String {
    var result = ""

    suspend {
        result = foo()
    }.startCoroutine(EmptyContinuation)

    return result
}
