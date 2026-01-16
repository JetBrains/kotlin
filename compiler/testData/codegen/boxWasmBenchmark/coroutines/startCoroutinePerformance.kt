// WITH_STDLIB
import kotlin.coroutines.*

// Performance test for startCoroutine function
// Measures the overhead of starting coroutines multiple times

var completionCount = 0

suspend fun simpleCoroutine(): String {
    return "OK"
}

fun box(): String {
    completionCount = 0
    
    // Start multiple coroutines to measure startCoroutine performance
    repeat(100) {
        val coroutine: suspend () -> String = ::simpleCoroutine
        
        coroutine.startCoroutine(object : Continuation<String> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext
            
            override fun resumeWith(result: Result<String>) {
                if (result.isSuccess && result.getOrNull() == "OK") {
                    completionCount++
                }
            }
        })
    }
    
    return if (completionCount == 100) "OK" else "FAIL: $completionCount"
}
