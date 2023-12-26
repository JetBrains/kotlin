// WITH_STDLIB
// WITH_COROUTINES
// JVM_ABI_K1_K2_DIFF: KT-63864

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
        postponedActions.add {
            x.resumeWithException(e)
        }

        COROUTINE_SUSPENDED
    }

    fun run(c: suspend Controller.() -> Unit) {
        c.startCoroutine(this, handleExceptionContinuation {
            exception = it
        })
        while (postponedActions.isNotEmpty()) {
            postponedActions[0]()
            postponedActions.removeAt(0)
        }
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    val controller = Controller()
    controller.run(c)

    if (controller.exception?.message != "OK") {
        throw RuntimeException("Unexpected result: ${controller.exception?.message}")
    }
}

fun commonThrow(t: Throwable) {
    throw t
}

suspend fun justContinue(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(Unit)

    COROUTINE_SUSPENDED
}

suspend fun Controller.test1() {
    justContinue()
    throw RuntimeException("OK")
}

suspend fun Controller.test2() {
    justContinue()
    commonThrow(RuntimeException("OK"))
}

suspend fun Controller.test3() {
    justContinue()
    suspendWithException(RuntimeException("OK"))
}

suspend fun Controller.test4() {
    justContinue()
    try {
        suspendWithException(RuntimeException("fail 1"))
    } catch (e: RuntimeException) {
        suspendWithException(RuntimeException("OK"))
    }
}

suspend fun Controller.test5() {
    justContinue()
    try {
        suspendWithException(Exception("OK"))
    } catch (e: RuntimeException) {
        suspendWithException(RuntimeException("fail 3"))
        throw RuntimeException("fail 4")
    }
}

fun box(): String {
    builder {
        test1()
    }

    builder {
        test2()
    }

    builder {
        test3()
    }

    builder {
        test4()
    }

    builder {
        test5()
    }

    return "OK"
}
