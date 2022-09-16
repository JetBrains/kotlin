// IGNORE_BACKEND: JVM
// WITH_COROUTINES
// WITH_STDLIB
// FILE: a.kt
fun interface A {
    suspend fun run()
}

// FILE: b.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.EmptyContinuation

suspend fun run(block: suspend () -> Unit) = A(block).run()

fun box(): String {
    suspend {
        run {}
    }.startCoroutine(EmptyContinuation)
    return "OK"
}
