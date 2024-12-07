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

suspend fun runA(block: suspend () -> Unit) = A(block).run()
suspend fun runC(block: suspend () -> Unit) = C(block).run()

fun box(): String {
    suspend {
        runA {}
        runC {}
        A { }.run()
        C { }.run()
    }.startCoroutine(EmptyContinuation)
    return "OK"
}

// FILE: c.kt
fun interface C {
    suspend fun run()
}
