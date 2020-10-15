// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR, JS_IR
// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.coroutines.*

fun <T> builder(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

suspend fun <T> runSuspend(c: suspend () -> T): T {
    return c()
}

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
suspend fun foo(): Result<String> = runSuspend {
    run { return@runSuspend Result.failure<String>(RuntimeException("OK")) }
}

fun box(): String {
    return try {
        builder { foo() }.getOrThrow()
        "FAIL"
    } catch (e: RuntimeException) {
        e.message!!
    }
}
