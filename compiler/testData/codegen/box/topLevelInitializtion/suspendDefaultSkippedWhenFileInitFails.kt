// ISSUE: KT-90564
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// WITH_STDLIB
// WITH_COROUTINES

// FILE: state.kt
var log = ""
val initializationFailure = IllegalStateException("init")

fun failInitialization(): Unit {
    log += "init;"
    throw initializationFailure
}

fun defaultValue(): Unit {
    log += "default;"
}

// FILE: target.kt
private val marker = failInitialization()

suspend fun target(unused: Unit = defaultValue()) {
    log += "body;"
}

// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    var completions = 0
    var failure: Throwable? = null
    suspend { target() }.startCoroutine(Continuation<Unit>(EmptyCoroutineContext) {
        completions++
        failure = it.exceptionOrNull()
        log += "failure;"
    })
    if (log != "init;failure;") return log
    if (completions != 1) return "completion count: $completions"
    var cause = failure ?: return "missing failure"
    while (cause.cause != null) cause = cause.cause!!
    if (cause !== initializationFailure) return "wrong failure: $cause"
    return "OK"
}
