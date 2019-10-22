// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
import helpers.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter
// Start coroutine call is possible
// Block is allowed to be called directly inside inline function

suspend inline fun test1(noinline c: suspend () -> Unit)  {
    val l : suspend () -> Unit = { c() }
    builder { l() }
}


suspend inline fun test2(noinline c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend inline fun test3(noinline c: suspend () -> Unit) {
    c()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun test4(noinline c: suspend () -> Unit) {
    val sr = object: SuspendRunnable {
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

suspend fun calculate() = "OK"

fun box(): String {
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
        test4 {
            res = "OK"
        }
    }
    if (res != "OK") return res
    res = "FAIL 5"
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
    res = "FAIL 6"
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
    res = "FAIL 7"
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
    res = "FAIL 8"
    builder {
        test4 {
            test4 {
                test4 {
                    test4 {
                        test4 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 9"
    builder {
        test1 {
            test2 {
                test3 {
                    test4 {
                        test1 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return res
}
