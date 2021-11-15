// WITH_STDLIB
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS
import helpers.*
import kotlin.coroutines.*

tailrec suspend fun foo(x: Int) {
    if (x == 0) return
    (return foo(x - 1))
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        foo(1000000)
    }
    return "OK"
}
