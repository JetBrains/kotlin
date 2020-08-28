// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var continuation: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation = it }

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
suspend fun signInFlowStepFirst(): Result<Unit> = try {
    Result.success(suspendMe())
} catch (e: Exception) {
    Result.failure(e)
}

fun box(): String {
    builder {
        signInFlowStepFirst()
    }
    continuation!!.resumeWithException(Exception("BOOYA"))
    return "OK"
}
