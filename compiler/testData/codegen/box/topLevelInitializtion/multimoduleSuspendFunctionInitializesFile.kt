// ISSUE: KT-90564
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// WITH_STDLIB
// WITH_COROUTINES

// MODULE: state
// FILE: state.kt
var log = ""
var initializationCount = 0

// MODULE: lib(state)
// FILE: target.kt
private val marker = run {
    initializationCount++
    log += "init;"
}

suspend fun target() {
    log += "body;"
}

// MODULE: main(state, lib)
// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    if (initializationCount != 0) return "initialized before call"
    var completions = 0
    val completion = Continuation<Unit>(EmptyCoroutineContext) {
        it.getOrThrow()
        completions++
    }
    val action: suspend () -> Unit = { target() }
    action.startCoroutine(completion)
    action.startCoroutine(completion)
    if (log != "init;body;body;") return log
    if (initializationCount != 1 || completions != 2) return "invalid counts"
    return "OK"
}
