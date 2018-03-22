// FILE: test.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Start coroutine call is possible
// Are suspend calls possible inside lambda matching to the parameter

interface SuspendRunnable {
    suspend fun run()
}

val EmptyContinuation = object: Continuation<Unit> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resume(value: Unit) {
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }
}

class Controller {
    var res = "FAIL 1"

    inline fun test1(noinline c: suspend Controller.() -> Unit)  {
        val l : suspend Controller.() -> Unit = { c() }
        builder(this) { l() }
    }

    inline fun test2(noinline c: suspend Controller.() -> Unit) {
        c.startCoroutine(this, EmptyContinuation)
    }

    inline fun test3(noinline c: suspend Controller.() -> Unit) {
        val sr = object : SuspendRunnable {
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

suspend fun calculate() = "OK"

fun box(): String {
    val controller = Controller()
    controller.test1 {
        res = calculate()
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 2"
    controller.test2 {
        res = "OK"
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 3"
    controller.test3 {
        res = "OK"
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 4"
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
    controller.res = "FAIL 5"
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
    controller.res = "FAIL 6"
    controller.test3 {
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
    controller.test1 {
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
    return controller.res
}
