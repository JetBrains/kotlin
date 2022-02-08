// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var lastSuspension: Continuation<String>? = null
    var result = "fail"
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        lastSuspension = x
        COROUTINE_SUSPENDED
    }

    fun hasNext() = lastSuspension != null
    fun next() {
        val x = lastSuspension!!
        lastSuspension = null
        x.resume("56")
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    val controller1 = Controller()
    val controller2 = Controller()

    c.startCoroutine(controller1, EmptyContinuation)
    c.startCoroutine(controller2, EmptyContinuation)

    runControllers(controller1, controller2)
}

// TODO: additional parameters are not supported yet
//fun builder2(coroutine c: Controller.(Long, String) -> Continuation<Unit>) {
//    val controller1 = Controller()
//    val controller2 = Controller()
//
//    c(controller1, 1234567890123456789L, "Q").resume(Unit)
//    c(controller2, 1234567890123456789L, "Q").resume(Unit)
//
//    runControllers(controller1, controller2)
//}

private fun runControllers(controller1: Controller, controller2: Controller) {
    while (controller1.hasNext()) {
        if (!controller2.hasNext()) throw RuntimeException("fail 1")

        if (controller1.lastSuspension === controller2.lastSuspension) throw RuntimeException("equal references")

        controller1.next()
        controller2.next()
    }

    if (controller2.hasNext()) throw RuntimeException("fail 2")

    if (controller1.result != "OK") throw RuntimeException("fail 3")
    if (controller2.result != "OK") throw RuntimeException("fail 4")
}

inline fun run(b: () -> Unit) {
    b()
}

fun box(): String {
    // with capture and params

    var x = "O"
    var y = "K"

    // inlined
    run {
        // no suspension
        builder {
            result = x + y
        }

        // 1 suspension
        builder {
            if (suspendHere() != "56") return@builder
            result = x + y
        }

        // 2 suspensions
        builder {
            if (suspendHere() != "56") return@builder
            suspendHere()
            result = x + y
        }
    }

    return "OK"
}
