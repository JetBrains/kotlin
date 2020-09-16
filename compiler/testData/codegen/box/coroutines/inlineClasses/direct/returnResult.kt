// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_FIR: JVM_IR

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
