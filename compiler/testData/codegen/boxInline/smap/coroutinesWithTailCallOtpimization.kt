// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// IGNORE_INLINER: IR

// FILE: inline.kt

interface SuspendRunnable {
    suspend fun run()
}

inline suspend fun inlineMe4(c: suspend () -> Unit) {
    c()
}

inline suspend fun inlineMe14(crossinline c: suspend () -> Unit) = inlineMe4(c)

// FILE: box.kt
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        inlineMe14 {

        }
    }

    return "OK"
}
