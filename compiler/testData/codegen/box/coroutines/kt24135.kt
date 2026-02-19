// WITH_STDLIB
// WITH_COROUTINES

// FILE: main.kt
import helpers.EmptyContinuation
import kotlin.coroutines.*

lateinit var c: Continuation<Unit>

fun builder(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation())
}

fun box(): String {
    val flow = foo {
        bar()
        result += "O"
    }
    builder {
        flow()
    }
    c.resume(Unit)
    return result
}

suspend fun bar() {
    return suspendCoroutine { cont: Continuation<Unit> ->
        c = cont
    }
}

// FILE: lib.kt
import kotlin.coroutines.*

var result = ""

inline fun foo(crossinline coroutine: suspend () -> Unit): suspend () -> Unit {
    return {
        coroutine.invoke()
        result += "K"
    }
}
