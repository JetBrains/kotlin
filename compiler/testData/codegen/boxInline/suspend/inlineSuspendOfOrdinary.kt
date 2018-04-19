// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

import COROUTINES_PACKAGE.*

// Block is allowed to be called inside the body of owner inline function
// suspend calls possible inside lambda matching to the parameter

suspend inline fun test(c: () -> Unit) {
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

inline fun transform(crossinline c: suspend () -> Unit) {
    builder { c() }
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL 1"
    builder {
        test {
            res = calculate()
        }
    }
    if (res != "OK") return res
    builder {
        test {
            transform {
                test {
                    res = calculate()
                }
            }
        }
    }
    return res
}
