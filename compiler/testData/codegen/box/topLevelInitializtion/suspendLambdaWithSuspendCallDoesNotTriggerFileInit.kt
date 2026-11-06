// ISSUE: KT-91167
// WITH_STDLIB

// FILE: state.kt
var initialized = false
var calls = 0

suspend fun immediate() {
    calls++
}

// FILE: callbacks.kt
val lazyProperty = run { initialized = true }

class Holder {
    val action: suspend () -> Unit = { immediate() }
}

// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    Holder().action.startCoroutine(
        Continuation<Unit>(EmptyCoroutineContext) { it.getOrThrow() }
    )
    val early = initialized
    lazyProperty
    if (!initialized || calls != 1) return "invalid setup"
    return if (early) "FAIL" else "OK"
}
