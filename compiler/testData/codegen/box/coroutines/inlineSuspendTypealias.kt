// JVM_ABI_K1_K2_DIFF: KT-68087
// WITH_STDLIB

import kotlin.coroutines.*

typealias Handler = suspend (String) -> Unit

suspend inline fun foo(handler: Handler)  {
    handler("OK")
}

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