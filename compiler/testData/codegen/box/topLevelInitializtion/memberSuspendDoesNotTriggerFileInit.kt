// ISSUE: KT-90564
// WITH_STDLIB
// WITH_COROUTINES

// FILE: state.kt
var initializationCount = 0
var calls = 0

// FILE: member.kt
val lazyProperty = run { ++initializationCount }

class Holder {
    suspend fun action() {
        calls++
    }
}

// FILE: main.kt
import kotlin.coroutines.*

fun box(): String {
    val holder = Holder()
    if (initializationCount != 0) return "construction initialized member file"
    var completions = 0
    val completion = Continuation<Unit>(EmptyCoroutineContext) {
        it.getOrThrow()
        completions++
    }
    val action: suspend () -> Unit = { holder.action() }
    action.startCoroutine(completion)
    action.startCoroutine(completion)
    if (calls != 2 || completions != 2) return "member did not complete"
    if (initializationCount != 0) return "member initialized its file"
    if (lazyProperty != 1 || initializationCount != 1) return "missing property initialization"
    return "OK"
}
