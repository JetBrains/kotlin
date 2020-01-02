// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
import helpers.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Start coroutine call is possible
// Are suspend calls possible inside lambda matching to the parameter

inline fun test1(noinline c: suspend () -> Unit)  {
    val l : suspend () -> Unit = { c() }
    builder { l() }
}

inline fun test2(noinline c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface SuspendRunnable {
    suspend fun run()
}

inline fun test3(noinline c: suspend () -> Unit) {
    val sr = object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    builder { sr.run() }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

suspend fun calculate() = "OK"

fun box(): String {
    var res = "FAIL 1"
    test1 {
        res = calculate()
    }
    if (res != "OK") return res
    res = "FAIL 2"
    test2 {
        res = "OK"
    }
    if (res != "OK") return res
    res = "FAIL 3"
    test3 {
        res = "OK"
    }
    if (res != "OK") return res
    res = "FAIL 4"
    test1 {
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
    test2 {
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
    test3 {
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
    test1 {
        test2 {
            test3 {
                test1 {
                    test2 {
                        test3 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return res
}
