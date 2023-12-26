// WITH_STDLIB
// WITH_COROUTINES
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var globalResult = ""
var wasCalled = false
class Controller {
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

    fun run(c: suspend Controller.() -> String) {
        c.startCoroutine(this, handleResultContinuation {
            globalResult = it
        })
        while (postponedActions.isNotEmpty()) {
            postponedActions[0]()
            postponedActions.removeAt(0)
        }
    }
}

fun builder(expectException: Boolean = false, c: suspend Controller.() -> String) {
    val controller = Controller()

    globalResult = "#"
    wasCalled = false
    if (!expectException) {
        controller.run(c)
    }
    else {
        try {
            controller.run(c)
            globalResult = "fail: exception was not thrown"
        } catch (e: Exception) {
            globalResult = e.message!!
        }
    }

    if (!wasCalled) {
        throw RuntimeException("fail wasCalled")
    }

    if (globalResult != "OK") {
        throw RuntimeException("fail $globalResult")
    }
}

fun commonThrow(t: Throwable) {
    throw t
}

fun box(): String {
    builder {
        try {
            suspendWithValue("<none>")
            suspendWithValue("OK")
        } catch (e: RuntimeException) {
            suspendWithValue("fail 1")
        } finally {
            suspendWithValue("ignored 1")
            wasCalled = true
        }
    }

    builder {
        try {
            suspendWithException(RuntimeException("M"))
        } catch (e: RuntimeException) {
            if (e.message != "M") throw RuntimeException("fail 2")
            suspendWithValue("OK")
        } finally {
            suspendWithValue("ignored 2")
            wasCalled = true
        }
    }

    builder(expectException = true) {
        try {
            suspendWithException(Exception("OK"))
        } catch (e: RuntimeException) {
            suspendWithValue("fail")
            throw RuntimeException("fail 3")
        } finally {
            suspendWithValue("ignored 3")
            wasCalled = true
        }
    }

    builder(expectException = true) {
        try {
            suspendWithException(Exception("OK"))
        } catch (e: RuntimeException) {
            suspendWithValue("fail")
            return@builder "xyz"
        } finally {
            suspendWithValue("ignored 4")
            wasCalled = true
        }
    }

    builder {
        try {
            suspendWithException(Exception("M2"))
        } catch (e: RuntimeException) {
            suspendWithValue("fail")
            throw RuntimeException("fail 4")
        } catch (e: Exception) {
            if (e.message != "M2") throw Exception("fail 5: ${e.message}")
            suspendWithValue("OK")
        } finally {
            suspendWithValue("ignored 4")
            wasCalled = true
        }
    }

    builder {
        try {
            suspendWithValue("123")
            commonThrow(RuntimeException("M3"))
            suspendWithValue("456")
        } catch (e: RuntimeException) {
            if (e.message != "M3") throw Exception("fail 6: ${e.message}")
            suspendWithValue("OK")
        } finally {
            suspendWithValue("ignored 5")
            wasCalled = true
        }
    }

    builder(expectException = true) {
        try {
            suspendWithValue("123")
            commonThrow(Exception("OK"))
            suspendWithValue("456")
        } catch (e: RuntimeException) {
            suspendWithValue("fail")
            throw RuntimeException("fail 7")
        } finally {
            suspendWithValue("ignored 6")
            wasCalled = true
        }
    }

    builder {
        try {
            suspendWithValue("123")
            commonThrow(Exception("M3"))
            suspendWithValue("456")
        } catch (e: RuntimeException) {
            suspendWithValue("fail")
            throw RuntimeException("fail 8")
        } catch (e: Exception) {
            if (e.message != "M3") throw Exception("fail 9: ${e.message}")
            suspendWithValue("OK")
        } finally {
            suspendWithValue("ignored 7")
            wasCalled = true
        }
    }

    return globalResult
}
