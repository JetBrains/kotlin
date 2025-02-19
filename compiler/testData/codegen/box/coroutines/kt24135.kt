// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

import helpers.EmptyContinuation
import kotlin.coroutines.*

var result = ""
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

inline fun foo(crossinline coroutine: suspend () -> Unit): suspend () -> Unit {
    return {
        coroutine.invoke()
        result += "K"
    }
}
