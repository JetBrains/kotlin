// WITH_STDLIB
// WITH_COROUTINES

// FILE: ResultProvider.kt
fun interface ResultProvider {
    suspend fun getResult(): Result<String>
}

// FILE: main.kt
import kotlin.coroutines.*
import helpers.*

var globalResult: String = "<empty>"

suspend fun run() {
    handleResult {
        Result.success("OK")
    }
}

suspend fun handleResult(resultProvider: ResultProvider) {
    globalResult = resultProvider.getResult().getOrThrow()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        run()
    }
    return globalResult
}
