// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import helpers.*

interface I {
    suspend fun foo(g: suspend String.() -> Unit)
}

fun builder0(f: suspend () -> Unit) {
    f.startCoroutine(EmptyContinuation)
}

fun <T> builder(f: suspend I.() -> T) {
    builder0 {
        f(object : I {
            override suspend fun foo(g: suspend String.() -> Unit) {
                g("OK")
                "Force non-tail call".length
            }
        })
    }
}

fun box(): String {
    var result = "Fail"
    builder {
        foo {
            result = this
        }
    }
    return result
}
