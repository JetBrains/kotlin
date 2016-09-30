// WITH_RUNTIME
var globalResult = ""
var wasCalled = false
class Controller {
    val postponedActions = ArrayList<() -> Unit>()

    suspend fun suspendWithValue(v: String, x: Continuation<String>) {
        postponedActions.add {
            x.resume(v)
        }
    }

    suspend fun suspendWithException(e: Exception, x: Continuation<String>) {
        postponedActions.add {
            x.resumeWithException(e)
        }
    }

    operator fun handleResult(x: String, c: Continuation<Nothing>) {
        globalResult = x
    }

    fun run(c: Controller.() -> Continuation<Unit>) {
        c(this).resume(Unit)
        while (postponedActions.isNotEmpty()) {
            postponedActions[0]()
            postponedActions.removeAt(0)
        }
    }
}

fun builder(expectException: Boolean = false, coroutine c: Controller.() -> Continuation<Unit>) {
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
            try {
                suspendWithValue("<ignored>")
                suspendWithValue("OK")
            }
            catch (e: RuntimeException) {
                suspendWithValue("fail 1")
            }
        } finally {
            wasCalled = true
        }
    }

    builder {
        try {
            try {
                suspendWithException(RuntimeException("M1"))
            }
            catch (e: RuntimeException) {
                if (e.message != "M1") throw RuntimeException("fail 2")
                wasCalled = true
                suspendWithValue("OK")
            }
        } catch (e: Exception) {
            suspendWithValue("fail 3")
        }
    }

    builder {
        try {
            try {
                suspendWithException(Exception("M2"))
            }
            catch (e: RuntimeException) {
                suspendWithValue("fail 4")
            } finally {
                wasCalled = true
            }
        } catch (e: Exception) {
            if (e.message != "M2") throw RuntimeException("fail 5")
            suspendWithValue("OK")
        }
    }

    return globalResult
}
