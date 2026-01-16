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

val coroutines: MutableList<Continuation<Unit>> = mutableListOf<Continuation<Unit>>()
val coroutines2: MutableList<Continuation<Unit>> = mutableListOf<Continuation<Unit>>()

var deepReached = 0
var deepCompletion = 0
suspend fun deep(n: Int) {
    if (n == 0) {
        ++deepReached
    } else {
        suspendCoroutine<Unit> { cont ->
            coroutines2.add(cont)
            coroutines[n - 1].resume(Unit)
            COROUTINE_SUSPENDED
        }
    }
    ++deepCompletion
}

fun box(): String {
    coroutines.clear()
    coroutines2.clear()
    deepReached = 0
    deepCompletion = 0
    val depth = 20
    val result: String? = null
    for (i in 1..depth) {
        builder {
            suspendCoroutine { cont ->
                coroutines.add(cont)
                COROUTINE_SUSPENDED
            }
            deep(i - 1)
        }
    }
    coroutines.last().resume(Unit)
    coroutines2.forEach { it.resume(Unit) }
    if (deepReached != 1) return "FAIL: deepReached=$deepReached"
    if (deepCompletion != depth) return "FAIL: deepCompletion=$deepCompletion"
    return "OK"
}
