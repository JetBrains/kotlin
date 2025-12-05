// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var exception: Throwable? = null
    val postponedActions = ArrayList<() -> Unit>()

    suspend fun suspendWithValue(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
        postponedActions.add {
            x.resume(v)
        }

        COROUTINE_SUSPENDED
    }

    suspend fun suspendWithException(e: Exception): String = suspendCoroutineUninterceptedOrReturn { x ->
        println("suspendWithException suspend body")
        postponedActions.add {
            println("before x.resumeWithException(e) where e.message=${e.message}")
//            println(x::class.qualifiedName)
            x.resumeWithException(e)
            println("after x.resumeWithException(e)")
        }

        COROUTINE_SUSPENDED
    }

    fun run(c: suspend Controller.() -> Unit) {
        println("before c.startCoroutine")
        c.startCoroutine(this, handleExceptionContinuation {
            println("handleExceptionContinuation lambda")
            exception = it
        })
        println("after c.startCoroutine")
        while (postponedActions.isNotEmpty()) {
            println("postponed action")
            postponedActions[0]()
            postponedActions.removeAt(0)
        }
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    println("builder start")
    val controller = Controller()
    println("before controller.run(c)")
    controller.run(c)

    println("controller.exception?.message=${controller.exception?.message}")
    if (controller.exception?.message != "OK") {
        throw RuntimeException("Unexpected result: ${controller.exception?.message}")
    }
    println("builder end")
}

fun commonThrow(t: Throwable) {
    throw t
}

fun box(): String {
//    builder {
//        throw RuntimeException("OK")
//    }
//
//    builder {
//        commonThrow(RuntimeException("OK"))
//    }
//
//    builder {
//        suspendWithException(RuntimeException("OK"))
//    }

    // ACTUAL
    // builder start
    // before controller.run(c)
    // before c.startCoroutine
    // suspendWithException suspend body
    // after c.startCoroutine
    // postponed action
    // before x.resumeWithException(e) where e.message=fail 1
    // after catch
    // suspendWithException suspend body
    // ./index.wasm:0x350d7: suspension error: unhandled tag

    // EXPECTED
    // builder start
    // before controller.run(c)
    // before c.startCoroutine
    // suspendWithException suspend body
    // after c.startCoroutine
    // postponed action
    // before x.resumeWithException(e) where e.message=fail 1
    // after catch
    // suspendWithException suspend body
    // after x.resumeWithException(e)
    // postponed action
    // before x.resumeWithException(e) where e.message=OK
    // handleExceptionContinuation lambda
    // after x.resumeWithException(e)
    // controller.exception?.message=OK
    // builder end
    println("test trycatch")
    builder {
        try {
            suspendWithException(RuntimeException("fail 1"))
        } catch (e: RuntimeException) {
            println("after catch")
            suspendWithException(RuntimeException("OK"))
        }
    }

//    println("test trycatch 2")
//    builder {
//        try {
//            suspendWithException(Exception("OK"))
//        } catch (e: RuntimeException) {
//            suspendWithException(RuntimeException("fail 3"))
//            throw RuntimeException("fail 4")
//        }
//    }

    return "OK"
}
