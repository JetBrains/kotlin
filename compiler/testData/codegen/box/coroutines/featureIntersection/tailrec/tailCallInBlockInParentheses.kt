// WITH_STDLIB
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS
import helpers.*
import kotlin.coroutines.*

tailrec suspend fun foo(x: Int) {
    return if (x > 0) {
        (foo(x - 1))
    }
    else Unit
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
