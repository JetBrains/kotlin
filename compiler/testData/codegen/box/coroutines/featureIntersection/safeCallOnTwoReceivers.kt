// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import kotlin.test.assertEquals

class A(val w: String) {
    suspend fun String.ext(): String = suspendCoroutineOrReturn {
        x ->
        x.resume(this + w)
        COROUTINE_SUSPENDED
    }
}

suspend fun A.coroutinebug(v: String?): String {
    val r = v?.ext()
    if (r == null) return "null"
    return r
}

suspend fun A.coroutinebug2(v: String?): String {
    val r = v?.ext() ?: "null"
    return r
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail 2"

    builder {
        val a = A("K")
        val x1 = a.coroutinebug(null)
        if (x1 != "null") throw RuntimeException("fail 1: $x1")

        val x2 = a.coroutinebug(null)
        if (x2 != "null") throw RuntimeException("fail 2: $x2")

        val x3 = a.coroutinebug2(null)
        if (x3 != "null") throw RuntimeException("fail 3: $x3")

        result = a.coroutinebug2("O")
    }

    return result
}
