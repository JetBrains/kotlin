// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class ManualContinuation : Continuation<Unit> {
    override val context: CoroutineContext = EmptyCoroutineContext
    var completed = false
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
        completed = true
    }
}

fun builder(c: suspend () -> Unit): ManualContinuation {
    val cont = ManualContinuation()
    c.startCoroutine(cont)
    return cont
}

fun box(): String {
    val iterations = 20
    val fibonacci = sequence {
        yield(1)
        yield(1)
        var a = 1
        var b = 1
        while (true) {
            yield(a + b)
            val temp = a
            a = b
            b += temp
        }
    }.iterator()
    var current = 0
    for (i in 1..iterations) {
        current = fibonacci.next()
    }
    return "OK"
}
