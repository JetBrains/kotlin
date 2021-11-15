// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: String)

interface I {
    suspend fun foo(s: String): IC
}

class D: I {
    override suspend fun foo(s: String): IC {
        return IC(s)
    }
}

class C(val d: I) : I by d


fun box(): String {
    var result = "FAIL"

    val d = D()

    builder {
        result = C(d).foo("OK").s
    }

    if (result != "OK") return "FAIL: $result"

    return result
}
