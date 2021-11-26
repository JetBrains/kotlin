// WITH_COROUTINES
// WITH_STDLIB
// SKIP_SOURCEMAP_REMAPPING
// FILE: test.kt
import kotlin.coroutines.*
import helpers.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// suspend calls possible inside lambda matching to the parameter

interface SuspendRunnable {
    suspend fun run()
}

class Controller {
    var res = "FAIL 1"

    suspend inline fun test1(crossinline c: suspend Controller.() -> Unit) {
        c()
    }

    suspend inline fun test2(crossinline c: suspend Controller.() -> Unit) {
        val l: suspend Controller.() -> Unit = { c() }
        l()
    }

    suspend inline fun test3(crossinline c: suspend Controller.() -> Unit) {
        val sr = object : SuspendRunnable {
            override suspend fun run() {
                c()
            }
        }
        sr.run()
    }
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
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
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 3"
    builder(controller) {
        test3 {
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 4"
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
    controller.res = "FAIL 5"
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
    controller.res = "FAIL 6"
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
    controller.res = "FAIL 7"
    builder(controller) {
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
    return controller.res
}
