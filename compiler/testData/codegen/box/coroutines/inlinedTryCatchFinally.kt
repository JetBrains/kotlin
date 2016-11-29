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

fun commonThrow(t: Throwable) {
    throw t
}

inline fun tryCatch(t: () -> String, onException: (Exception) -> String) =
        try {
            t()
        } catch (e: RuntimeException) {
            onException(e)
        }

inline fun tryCatchFinally(t: () -> String, onException: (Exception) -> String, f: () -> Unit) =
        try {
            t()
        } catch (e: RuntimeException) {
            onException(e)
        } finally {
            f()
        }

fun box(): String {
    builder {
        tryCatch(
                {
                    suspendWithValue("<ignored>")
                    wasCalled = true
                    suspendWithValue("OK")
                },
                { e ->
                    suspendWithValue("fail 1")
                }
        )
    }

    builder {
        tryCatch(
                {
                    suspendWithException(RuntimeException("M"))
                },
                { e ->
                    if (e.message != "M") throw RuntimeException("fail 2")
                    wasCalled = true
                    suspendWithValue("OK")
                }
        )
    }

    builder {
        tryCatchFinally(
                {
                    suspendWithValue("<none>")
                    wasCalled = true
                    suspendWithValue("OK")
                },
                {
                    suspendWithValue("fail 1")
                },
                {
                    suspendWithValue("ignored 1")
                    wasCalled = true
                }
        )
    }

    builder {
        tryCatchFinally(
                {
                    suspendWithException(RuntimeException("M"))
                },
                { e ->
                    if (e.message != "M") throw RuntimeException("fail 2")
                    suspendWithValue("OK")
                },
                {
                    suspendWithValue("ignored 2")
                    wasCalled = true
                }
        )
    }

    builder {
        tryCatchFinally(
                {
                    if (suspendWithValue("56") == "56") return@tryCatchFinally "OK"
                    suspendWithValue("fail 4")
                },
                {
                    suspendWithValue("fail 5")
                },
                {
                    suspendWithValue("ignored 3")
                    wasCalled = true
                }
        )
    }

    return globalResult
}
