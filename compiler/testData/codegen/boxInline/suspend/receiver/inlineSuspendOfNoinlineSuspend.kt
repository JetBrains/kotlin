// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// SKIP_SOURCEMAP_REMAPPING
import COROUTINES_PACKAGE.*
import helpers.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter
// Start coroutine call is possible
// Block is allowed to be called directly inside inline function

interface SuspendRunnable {
    suspend fun run()
}


class Controller {
    var res = "FAIL 1"

    suspend inline fun test1(noinline c: suspend Controller.() -> Unit)  {
        val l : suspend Controller.() -> Unit = { c() }
        l()
    }

    suspend inline fun test2(noinline c: suspend Controller.() -> Unit) {
        c.startCoroutine(this, EmptyContinuation)
    }

    suspend inline fun test3(noinline c: suspend Controller.() -> Unit) {
        c()
    }

    suspend inline fun test4(noinline c: suspend Controller.() -> Unit) {
        val sr = object: SuspendRunnable {
            override suspend fun run() {
                c()
            }
        }
        sr.run()
    }
}

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, EmptyContinuation)
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

suspend fun calculate() = "OK"

fun box(): String {
    val controller = Controller()
    builder(controller) {
        test1 {
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 2"
    builder(controller) {
        test2 {
            res = "OK"
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 3"
    builder(controller) {
        test3 {
            res = "OK"
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 4"
    builder(controller) {
        test4 {
            res = "OK"
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 5"
    builder(controller) {
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
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 6"
    builder(controller) {
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
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 7"
    builder(controller) {
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
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 8"
    builder(controller) {
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
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 9"
    builder(controller) {
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
    return controller.res
}
