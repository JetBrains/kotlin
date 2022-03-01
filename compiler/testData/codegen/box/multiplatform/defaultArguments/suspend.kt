// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: default argument mapping in MPP isn't designed yet
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB
// WITH_COROUTINES

// FILE: lib.kt

expect interface I {
    suspend fun f(p: Int = 1): String
}

// FILE: main.kt
import kotlin.coroutines.*
import helpers.*

actual interface I {
    actual suspend fun f(p: Int): String
}

class II : I {
    override suspend fun f(p: Int): String =
        if (p == 1) "OK" else "Fail: $p"
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = II().f()
    }
    return res
}
