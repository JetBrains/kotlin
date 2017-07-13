package library

import kotlin.coroutines.experimental.*

var continuation: Continuation<Unit>? = null
val sb = java.lang.StringBuilder()

fun append(s: String) { sb.append(s) }

val libraryResult: String get() = sb.toString()

// this is an inline tail-suspend function that should work properly despite being compiler by
// compiler before 1.1.4 version that did not include suspension marks into bytecode.
// In order to test that it works properly it shall actually suspend during its execution
inline suspend fun foo(block: () -> Unit) {
    append("(foo)")
    block()
    return suspendCoroutine<Unit> { continuation = it }
}

suspend fun bar() {
    append("(bar)")
    return suspendCoroutine<Unit> { continuation = it }
}

fun resumeLibrary() {
    continuation?.let {
        continuation = null
        it.resume(Unit)
    }
}
