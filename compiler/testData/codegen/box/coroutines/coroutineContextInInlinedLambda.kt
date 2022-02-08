// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

inline fun inlinedLambda(block: () -> Unit) {
    return block()
}

suspend fun useInlined(): Boolean {
    inlinedLambda { return coroutineContext === EmptyCoroutineContext }
    return false
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL 1"
    builder {
        if (useInlined())
            res = "OK"
    }
    if (res != "OK") return res
    res = "FAIL 2"
    builder {
        inlinedLambda {
            res = if (coroutineContext === EmptyCoroutineContext) "OK" else "FAIL 3"
        }
    }
    return res
}
