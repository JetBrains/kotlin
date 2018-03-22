// FILE: test.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

suspend inline fun test1(c: () -> Unit) {
    c()
}

suspend inline fun test2(c: suspend () -> Unit) {
    c()
}

suspend inline fun test3(crossinline c: suspend() -> Unit) {
    c()
}

suspend inline fun test4(crossinline c: suspend() -> Unit) {
    val l : suspend () -> Unit = { c() }
    l()
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun test5(crossinline c: suspend() -> Unit) {
    val sr = object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    sr.run()
}

// FILE: box.kt

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
import kotlin.coroutines.experimental.jvm.internal.*

object EmptyContinuation: Continuation<Unit> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resume(value: Unit) {
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var continuationChanged = true
var savedContinuation: Continuation<Unit>? = null

suspend inline fun saveContinuation() = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
    savedContinuation = c
    Unit
}

suspend inline fun checkContinuation(continuation: Continuation<Unit>) = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
    continuationChanged = (continuation !== c)
    Unit
}

fun box() : String {
    builder {
        saveContinuation()
        test1 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 1"
    continuationChanged = true
    builder {
        saveContinuation()
        test2 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 2"
    continuationChanged = true
    builder {
        saveContinuation()
        test3 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 3"
    continuationChanged = false
    builder {
        saveContinuation()
        test4 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (!continuationChanged) return "FAIL 4"
    continuationChanged = false
    builder {
        saveContinuation()
        test5 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (!continuationChanged) return "FAIL 5"
    return "OK"
}
