// IGNORE_BACKEND: JVM
// WITH_STDLIB
// KT-44849

import kotlin.coroutines.*

var result = "Fail"

class Wrapper(val action: suspend () -> Unit) {
    init {
        action.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    }
}

suspend fun some(a: String = "OK") {
    result = a
}

fun box(): String {
    Wrapper(::some)
    return result
}
