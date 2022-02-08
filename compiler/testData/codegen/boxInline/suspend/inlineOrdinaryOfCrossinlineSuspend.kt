// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt

import kotlin.coroutines.*
import helpers.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter

inline fun test1(crossinline runner: suspend () -> Unit)  {
    val l : suspend () -> Unit = { runner() }
    builder { l() }
}

interface SuspendRunnable {
    suspend fun run()
}

inline fun test2(crossinline c: suspend () -> Unit) {
    val sr = object: SuspendRunnable {
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
suspend fun calculate() = "OK"

fun box(): String {
    var res = "FAIL 1"
    test1 {
        res = calculate()
    }
    if (res != "OK") return res
    res = "FAIL 2"
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
    res = "FAIL 3"
    test2 {
        res = calculate()
    }
    if (res != "OK") return res
    res = "FAIL 4"
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
    res = "FAIL 5"
    test1 {
        test2 {
            test1 {
                test2 {
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
