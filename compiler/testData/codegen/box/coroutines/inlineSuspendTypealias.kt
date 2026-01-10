// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
typealias Handler = suspend (String) -> Unit

suspend inline fun foo(handler: Handler)  {
    handler("OK")
}

// FILE: main.kt
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var result = "FAIL"
    builder {
        foo {
            result = it
        }
    }
    return result
}
