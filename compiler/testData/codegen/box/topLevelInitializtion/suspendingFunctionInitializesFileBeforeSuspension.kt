// ISSUE: KT-90564
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// WITH_STDLIB
// WITH_COROUTINES

// FILE: state.kt
import kotlin.coroutines.*

var log = ""
var initializationCount = 0
var savedContinuation: Continuation<Unit>? = null

// FILE: target.kt
import kotlin.coroutines.*

private val marker = run {
    initializationCount++
    log += "init;"
}

suspend fun target() {
    log += "before;"
    suspendCoroutine<Unit> { savedContinuation = it }
    log += "after;"
}

// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    if (initializationCount != 0) return "initialized before call"
    var completions = 0
    val completion = Continuation<Unit>(EmptyCoroutineContext) {
        it.getOrThrow()
        completions++
    }
    suspend { target() }.startCoroutine(completion)
    if (log != "init;before;") return "before resume: $log"
    if (initializationCount != 1 || completions != 0) return "invalid suspended state"
    val continuation = savedContinuation ?: return "missing continuation"
    savedContinuation = null
    continuation.resume(Unit)
    if (log != "init;before;after;") return "after resume: $log"
    if (initializationCount != 1 || completions != 1) return "invalid completed state"
    return "OK"
}
