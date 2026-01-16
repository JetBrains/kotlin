// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// Performance test for startCoroutineUninterceptedOrReturn function
// Measures the overhead of starting coroutines without interception

var completionCount = 0

suspend fun simpleCoroutine(): String {
    return "OK"
}

fun box(): String {
    completionCount = 0
    
    // Start multiple coroutines using startCoroutineUninterceptedOrReturn
    repeat(100) {
        val coroutine: suspend () -> String = ::simpleCoroutine
        
        coroutine.startCoroutineUninterceptedOrReturn(object : Continuation<String> {
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
