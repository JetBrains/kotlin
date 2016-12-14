// WITH_RUNTIME
// WITH_COROUTINES
var globalResult = ""
var wasCalled = false
class Controller {
    val postponedActions = ArrayList<() -> Unit>()

    suspend fun suspendWithValue(v: String): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resume(v)
        }

        SUSPENDED
    }

    suspend fun suspendWithException(e: Exception): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resumeWithException(e)
        }

        SUSPENDED
    }

    fun run(c: @Suspend() (Controller.() -> String)) {
        c.startCoroutine(this, handleResultContinuation {
            globalResult = it
        })
        while (postponedActions.isNotEmpty()) {
            postponedActions[0]()
            postponedActions.removeAt(0)
        }
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(expectException: Boolean = false, c: @Suspend() (Controller.() -> String)) {
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
