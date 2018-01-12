// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun twoReturns(c: suspend () -> Unit) {
    return c()
    throw RuntimeException("FAIL 1")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        twoReturns {
            res = "OK"
        }
    }
    return res
}
