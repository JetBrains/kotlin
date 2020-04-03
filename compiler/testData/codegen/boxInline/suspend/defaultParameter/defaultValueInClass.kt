// FILE: test.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
class C {
    suspend inline fun test(default: C = this, lambda: suspend () -> String) = lambda()
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

var res = "fail"

fun box() : String {
    suspend {
        res = C().test { "OK" }
    }.startCoroutine(EmptyContinuation)
    return res
}
