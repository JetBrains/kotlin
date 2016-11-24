// WITH_RUNTIME
class Controller {
    var exception: Throwable? = null
    val postponedActions = ArrayList<() -> Unit>()

    suspend fun suspendWithValue(v: String): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resume(v)
        }
    }

    suspend fun suspendWithException(e: Exception): String = suspendWithCurrentContinuation { x ->
        postponedActions.add {
            x.resumeWithException(e)
        }
    }

    operator fun handleException(t: Throwable, c: Continuation<Nothing>) {
        exception = t
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

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    controller.run(c)

    if (controller.exception?.message != "OK") {
        throw RuntimeException("Unexpected result: ${controller.exception?.message}")
    }
}

fun commonThrow(t: Throwable) {
    throw t
}

fun box(): String {
    builder {
        throw RuntimeException("OK")
    }

    builder {
        commonThrow(RuntimeException("OK"))
    }

    builder {
        suspendWithException(RuntimeException("OK"))
    }

    builder {
        try {
            suspendWithException(RuntimeException("fail 1"))
        } catch (e: RuntimeException) {
            suspendWithException(RuntimeException("OK"))
        }
    }

    builder {
        try {
            suspendWithException(Exception("OK"))
        } catch (e: RuntimeException) {
            suspendWithException(RuntimeException("fail 3"))
            throw RuntimeException("fail 4")
        }
    }

    return "OK"
}
