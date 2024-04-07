// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect interface I {
    suspend fun f(p: Int = 1): String
}

// MODULE: main()()(common)
// WITH_COROUTINES
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
