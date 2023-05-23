// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
// WITH_COROUTINES

// MODULE: common
// FILE: common.kt

expect interface I {
    suspend fun f(p: Int = 1): String
}

// MODULE: main()()(common)
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
