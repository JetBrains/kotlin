// FILE: test.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*

inline suspend fun foo(crossinline a: suspend () -> Unit, crossinline b: suspend () -> Unit) {
    var x = "OK"
    bar { x; a(); b() }
}

inline suspend fun bar(crossinline l: suspend () -> Unit) {
    val c : suspend () -> Unit = { l() }
    c()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

// FILE: box.kt

fun box(): String {
    var y = "fail"
    builder {
        foo({ y = "O" }) { y += "K" }
    }
    return y
}