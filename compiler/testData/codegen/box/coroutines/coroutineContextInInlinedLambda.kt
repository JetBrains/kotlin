// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
import kotlin.coroutines.*

inline fun inlinedLambda(block: () -> Unit) {
    return block()
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

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
