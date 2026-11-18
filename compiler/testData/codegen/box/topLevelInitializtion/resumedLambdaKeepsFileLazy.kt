// WITH_STDLIB
// WITH_COROUTINES

// FILE: state.kt
import kotlin.coroutines.*

var initializationCount = 0
var events = ""
var savedContinuation: Continuation<Unit>? = null
var completionCount = 0
var failure: Throwable? = null

// FILE: callbacks.kt
import kotlin.coroutines.*

val lazyProperty = run { ++initializationCount }

class Holder {
    val action: suspend () -> Unit = {
        events += "before;"
        suspendCoroutine<Unit> { savedContinuation = it }
        events += "after;"
    }
}

// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    val holder = Holder()
    if (initializationCount != 0) return "construction initialized callback file"
    holder.action.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            completionCount++
            failure = result.exceptionOrNull()
        }
    })
    if (events != "before;") return "did not suspend: $events"
    if (completionCount != 0) return "completed before resume"
    if (initializationCount != 0) return "suspension initialized callback file"
    val continuation = savedContinuation ?: return "missing continuation"
    savedContinuation = null
    continuation.resume(Unit)
    if (events != "before;after;") return "did not resume: $events"
    if (completionCount != 1) return "completion count: $completionCount"
    if (failure != null) return "coroutine failed: $failure"
    if (initializationCount != 0) return "resume initialized callback file"
    if (lazyProperty != 1) return "lazy property value"
    if (initializationCount != 1) return "missing initialization"
    return "OK"
}
