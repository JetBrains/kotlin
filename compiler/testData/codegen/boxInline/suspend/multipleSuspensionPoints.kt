// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

suspend inline fun inlineMe(c: suspend () -> Unit) {
    c()
    c()
    c()
    c()
    c()
}
suspend inline fun noinlineMe(noinline c: suspend () -> Unit) {
    c()
    c()
    c()
    c()
    c()
}
suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    c()
    c()
    c()
    c()
    c()
}

// FILE: inlineSite.kt

import kotlin.coroutines.experimental.*

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

var i = 0;
var j = 0;
var k = 0;

suspend fun calculateI() {
    i++
}

suspend fun calculateJ() {
    j++
}

suspend fun calculateK() {
    k++
}

suspend fun inlineSite() {
    inlineMe {
        calculateI()
        calculateI()
    }
    noinlineMe {
        calculateJ()
        calculateJ()
    }
    crossinlineMe {
        calculateK()
        calculateK()
    }
}

fun box(): String {
    builder {
        inlineSite()
    }
    if (i != 10) return "FAIL I"
    if (j != 10) return "FAIL J"
    if (k != 10) return "FAIL K"
    return "OK"
}