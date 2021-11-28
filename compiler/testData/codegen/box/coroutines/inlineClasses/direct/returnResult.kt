// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
suspend fun signInFlowStepFirst(): Result<String> = try {
    Result.success("OK")
} catch (e: Exception) {
    Result.failure(e)
}

fun box(): String {
    builder {
        val res: Result<String> = signInFlowStepFirst()
        if (res.getOrThrow() != "OK") error("FAIL")
    }
    return "OK"
}
