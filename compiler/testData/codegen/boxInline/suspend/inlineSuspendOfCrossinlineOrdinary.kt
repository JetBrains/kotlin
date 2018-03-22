// FILE: test.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)

suspend inline fun test1(crossinline c: () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: () -> Unit) {
    val l = { c() }
    l()
}

suspend inline fun test3(crossinline c: () -> Unit) {
    val r = object: Runnable {
        override fun run() {
            c()
        }
    }
    r.run()
}

inline fun transform(crossinline c: suspend () -> Unit) {
    builder { c() }
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

import kotlin.coroutines.experimental.*

fun box() : String {
    var res = "FAIL 1"
    builder {
        test1 {
            res = "OK"
        }
    }
    if (res != "OK") return res

    res = "FAIL 2"
    builder {
        test2 {
            res = "OK"
        }
    }
    if (res != "OK") return res

    res = "FAIL 3"
    builder {
        test3 {
            res = "OK"
        }
    }
    if (res != "OK") return res

    res = "FAIL 4"
    builder {
        test1 {
            transform {
                test1 {
                    res = "OK"
                }
            }
        }
    }
    if (res != "OK") return res

    res = "FAIL 5"
    builder {
        test2 {
            transform {
                test2 {
                    res = "OK"
                }
            }
        }
    }
    if (res != "OK") return res

    res = "FAIL 6"
    builder {
        test3 {
            transform {
                test3 {
                    res = "OK"
                }
            }
        }
    }
    return res
}
