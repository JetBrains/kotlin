// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// SKIP_SOURCEMAP_REMAPPING
import COROUTINES_PACKAGE.*
import helpers.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter

interface SuspendRunnable {
    suspend fun run()
}

class Controller {
    var res = "FAIL 1"

    inline fun test1(crossinline runner: suspend Controller.() -> Unit)  {
        val l : suspend Controller.() -> Unit = { runner() }
        builder(this) { l() }
    }

    inline fun test2(crossinline c: suspend Controller.() -> Unit) {
        val sr = object: SuspendRunnable {
            override suspend fun run() {
                c()
            }
        }
        builder(this) { sr.run() }
    }
}

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, EmptyContinuation)
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

suspend fun calculate() = "OK"

fun box(): String {
    val controller = Controller()
    controller.test1 {
        res = calculate()
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 2"
    controller.test1 {
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
    controller.res = "FAIL 3"
    controller.test2 {
        res = calculate()
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 4"
    controller.test2 {
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
    controller.res = "FAIL 5"
    controller.test1 {
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
    return controller.res
}
