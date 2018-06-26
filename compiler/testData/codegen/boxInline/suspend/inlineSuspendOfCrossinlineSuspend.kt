// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
import helpers.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// suspend calls possible inside lambda matching to the parameter

suspend inline fun test1(crossinline c: suspend () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun test3(crossinline c: suspend () -> Unit) {
    val sr = object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    sr.run()
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL 1"
    builder {
        test1 {
            res = calculate()
        }
    }
    if (res != "OK") return res
    res = "FAIL 2"
    builder {
        test2 {
            res = calculate()
        }
    }
    if (res != "OK") return res
    res = "FAIL 3"
    builder {
        test3 {
            res = calculate()
        }
    }
    if (res != "OK") return res
    res = "FAIL 4"
    builder {
        test1 {
            test1 {
                test1 {
                    test1 {
                        test1 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 5"
    builder {
        test2 {
            test2 {
                test2 {
                    test2 {
                        test2 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 6"
    builder {
        test3 {
            test3 {
                test3 {
                    test3 {
                        test3 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 7"
    builder {
        test1 {
            test2 {
                test3 {
                    test1 {
                        test2 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return res
}
