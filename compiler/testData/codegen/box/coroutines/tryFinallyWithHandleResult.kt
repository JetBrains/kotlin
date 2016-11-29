// IGNORE_BACKEND: JS
// WITH_RUNTIME
var globalResult = ""
var wasCalled = false
class Controller {
    val postponedActions = mutableListOf<() -> Unit>()

    suspend fun suspendWithValue(v: String): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resume(v)
        }

        Suspend
    }

    suspend fun suspendWithException(e: Exception): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resumeWithException(e)
        }

        Suspend
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

    // INTERCEPT_RESUME_PLACEHOLDER
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

fun commonThrow() {
    throw RuntimeException("OK")
}

fun box(): String {
    builder {
        try {
            suspendWithValue("OK")
        } finally {
            if (suspendWithValue("G") != "G") throw RuntimeException("fail 1")
            wasCalled = true
        }
    }

    builder(expectException = true) {
        try {
            suspendWithException(RuntimeException("OK"))
        } finally {
            if (suspendWithValue("G") != "G") throw RuntimeException("fail 2")
            wasCalled = true
        }
    }

    builder(expectException = true) {
        try {
            suspendWithValue("OK")
            commonThrow()
            suspendWithValue("OK")
        } finally {
            if (suspendWithValue("G") != "G") throw RuntimeException("fail 3")
            wasCalled = true
        }
    }

    return globalResult
}
