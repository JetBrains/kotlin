// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
import kotlin.coroutines.*

suspend fun <T> withCorutine(block: suspend () -> Unit): Unit {
    block()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

inline fun f(): Int {
    if (42 != 42) return 12345
    return 67890
}

// FILE: main.kt
fun box(): String {
    builder {
        check(f() == 67890)
    }
    return "OK"
}