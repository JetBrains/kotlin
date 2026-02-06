import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var isLocked = false

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

private suspend fun lock() {
    isLocked = true
}

private fun unlock() {
    if (isLocked) {
        isLocked = false
    } else {
        throw IllegalStateException("Not locked")
    }
}

private suspend inline fun <T> withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}

fun box(): String {
    var error: Exception? = null
    builder {
        try {
            withLock {}
            throw Exception("seems fine")
        } catch (e: Exception) {
            error = e
        }
    }

    return if (error !is IllegalStateException) "OK" else "fail"
}