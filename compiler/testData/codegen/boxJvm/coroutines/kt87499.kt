import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

private val tasks = ArrayDeque<() -> Unit>()

private suspend fun yieldOnce(): Unit = suspendCoroutine { continuation ->
    tasks.addLast { continuation.resume(Unit) }
}

private fun runBlocking(block: suspend () -> Unit) {
    var completed = false
    var failure: Throwable? = null

    block.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            completed = true
            failure = result.exceptionOrNull()
        }
    })

    while (!completed) {
        val task = tasks.removeFirstOrNull() ?: error("Coroutine suspended without a queued continuation")
        task()
    }

    failure?.let { throw it }
}

private suspend fun launchedEffect(block: suspend () -> Unit) {
    yieldOnce()
    block()
}

@Suppress("UNCHECKED_CAST")
private suspend fun animateScrollTo(v: Int, spec: String = ""): Unit =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        tasks.addLast {
            (continuation as Continuation<Any?>).resume(v.toFloat())
        }
        COROUTINE_SUSPENDED
    }

fun box(): String {
    runBlocking {
        for (branch in 0..2) {
            launchedEffect {
                when (branch) {
                    1 -> animateScrollTo(100)   // omit default -> $default; Unit-typed, Float at runtime
                    2 -> animateScrollTo(0)
                    else -> Unit
                }
            }
        }
    }
    return "OK"
}
