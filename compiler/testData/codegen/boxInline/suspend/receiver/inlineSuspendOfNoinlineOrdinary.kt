// IGNORE_BACKEND: JS
// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt
import kotlin.coroutines.*
import helpers.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)

// Needed for JS compatibility
interface Runnable {
    fun run(): Unit
}

class Controller {
    var res = "FAIL 1"

    suspend inline fun test1(noinline c: Controller.() -> Unit) {
        c()
    }

    suspend inline fun test2(noinline c: Controller.() -> Unit) {
        val l = { c() }
        l()
    }

    suspend inline fun test3(noinline c: Controller.() -> Unit) {
        val r = object: Runnable {
            override fun run() {
                c()
            }
        }
        r.run()
    }

    inline fun transform(crossinline c: suspend Controller.() -> Unit) {
        builder(this) { c() }
    }
}

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, EmptyContinuation)
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun box() : String {
    val controller = Controller()
    builder(controller) {
        test1 {
            res = "OK"
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
        test1 {
            transform {
                test1 {
                    res = "OK"
                }
            }
        }
    }
    if (controller.res != "OK") return controller.res

    controller.res = "FAIL 5"
    builder(controller) {
        test2 {
            transform {
                test2 {
                    res = "OK"
                }
            }
        }
    }
    if (controller.res != "OK") return controller.res

    controller.res = "FAIL 6"
    builder(controller) {
        test3 {
            transform {
                test3 {
                    res = "OK"
                }
            }
        }
    }
    return controller.res
}
