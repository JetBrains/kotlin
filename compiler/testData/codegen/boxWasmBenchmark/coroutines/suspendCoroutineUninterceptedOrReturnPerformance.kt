// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// Performance test for suspendCoroutineUninterceptedOrReturn function
// Measures the overhead of suspending and resuming coroutines

var resumeCount = 0

suspend fun suspendAndResume(): String = suspendCoroutineUninterceptedOrReturn { continuation ->
    // Immediately resume to measure the suspension/resumption overhead
    continuation.resume("OK")
    COROUTINE_SUSPENDED
}

fun box(): String {
    resumeCount = 0
    var result = "FAIL"
    
    // Perform multiple suspend/resume cycles
    val coroutine: suspend () -> String = {
        var accumulated = ""
        repeat(10) {
            val value = suspendAndResume()
            if (value == "OK") {
                resumeCount++
            }
            accumulated = value
        }
        accumulated
    }
    
    coroutine.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext
        
        override fun resumeWith(value: Result<String>) {
            result = value.getOrNull() ?: "FAIL"
        }
    })
    
    return if (resumeCount == 10 && result == "OK") "OK" else "FAIL: $resumeCount"
}
