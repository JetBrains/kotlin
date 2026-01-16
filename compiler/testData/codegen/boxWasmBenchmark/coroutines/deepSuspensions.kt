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
var deepCoroutine: Continuation<Unit>? = null

var deepReached = 0
var deepCompletion = 0
var deepSuspensionsCount = 0
suspend fun deep(n: Int, deepSuspensionsNum: Int) {
    if (n == 0) {
        ++deepReached
        for (i in 1..deepSuspensionsNum) {
            suspendCoroutine<Unit> { cont ->
                ++deepSuspensionsCount
                deepCoroutine = cont
                COROUTINE_SUSPENDED
            }
        }
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
    deepSuspensionsCount = 0
    val depth = 20
    val deepSuspensionsNum = 20
    val result: String? = null
    for (i in 1..depth) {
        builder {
            suspendCoroutine { cont ->
                coroutines.add(cont)
                COROUTINE_SUSPENDED
            }
            deep(i - 1, deepSuspensionsNum)
        }
    }
    coroutines.last().resume(Unit)
    for (i in 1..deepSuspensionsNum) {
        deepCoroutine!!.resume(Unit)
    }
    coroutines2.forEach { it.resume(Unit) }
    if (deepSuspensionsCount != deepSuspensionsNum) return "FAIL: deepSuspensionsCount=$deepSuspensionsCount"
    if (deepReached != 1) return "FAIL: deepReached=$deepReached"
    if (deepCompletion != depth) return "FAIL: deepCompletion=$deepCompletion"
    return "OK"
}
