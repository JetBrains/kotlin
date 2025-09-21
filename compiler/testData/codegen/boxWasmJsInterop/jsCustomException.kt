// TARGET_BACKEND: WASM

fun throwCustomNamedJsError(): Int = js("""
{
    const e = new Error("Custom error message");
    e.name = "MyCustomError";
    throw e;
}
""")

fun jsGetName(v: JsAny?): String? = js("""(v != null && typeof v === "object" && "name" in v) ? v.name : null""")

private fun assertCustomJsError(e: JsException, caller: String): Boolean {
    val stacktrace = e.stackTraceToString()
    return e.message == "Custom error message" &&
            jsGetName(e.thrownValue) == "MyCustomError" &&
            stacktrace.contains("throwCustomNamedJsError") &&
            (stacktrace.contains("<main>.$caller") || stacktrace.contains("$caller@<main>")) &&
            (stacktrace.contains("<main>.box") || stacktrace.contains("box@<main>"))
}

inline fun <reified T : Throwable> wasThrown(fn: () -> Any?): Boolean {
    try {
        fn()
        return false
    } catch (e: Throwable) {
        return e is T
    }
}

fun customJsErrorWithFinally(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } finally {
        return true
    }
}

fun customJsErrorWithCatchThrowable(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsError(e, "customJsErrorWithCatchThrowable")
    }
}

fun customJsErrorWithCatchJsException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: JsException) {
        return assertCustomJsError(e, "customJsErrorWithCatchJsException")
    }
}

fun customJsErrorWithCatchIllegalStateException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        return false
    }
    return true
}

fun customJsErrorWithCatchThrowableAndFinally(): Boolean {
    var ex: Throwable? = null
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        ex = e
    } finally {
        return ex is JsException && assertCustomJsError(ex as JsException, "customJsErrorWithCatchThrowableAndFinally")
    }
}

fun customJsErrorWithCatchJsExceptionAndFinally(): Boolean {
    var ex: JsException? = null
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: JsException) {
        ex = e
    } finally {
        return ex != null && assertCustomJsError(ex!!, "customJsErrorWithCatchJsExceptionAndFinally")
    }
}

fun customJsErrorWithCatchIllegalStateExceptionAndFinally(): Boolean {
    var ex: IllegalStateException? = null
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        ex = e
    } finally {
        return ex == null
    }
}

fun customJsErrorWithCatchJsExceptionAndThrowable(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: JsException) {
        return assertCustomJsError(e, "customJsErrorWithCatchJsExceptionAndThrowable")
    } catch (e: Throwable) {
        return false
    }
}

fun customJsErrorWithCatchIllegalStateExceptionAndThrowable(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsError(e, "customJsErrorWithCatchIllegalStateExceptionAndThrowable")
    }
}

fun customJsErrorWithCatchThrowableAndJsException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsError(e, "customJsErrorWithCatchThrowableAndJsException")
    } catch (e: JsException) {
        return false
    }
}

fun customJsErrorWithCatchIllegalStateExceptionAndJsException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        return false
    } catch (e: JsException) {
        return assertCustomJsError(e, "customJsErrorWithCatchIllegalStateExceptionAndJsException")
    }
}

fun customJsErrorWithCatchJsExceptionAndThrowableAndFinally(): Boolean {
    var ex: JsException? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: JsException) {
        ex = e
        caught = true
    } catch (e: Throwable) {
        ex = null
        caught = true
    } finally {
        return caught && ex != null && assertCustomJsError(ex!!, "customJsErrorWithCatchJsExceptionAndThrowableAndFinally")
    }
}

fun customJsErrorWithCatchIllegalStateExceptionAndThrowableAndFinally(): Boolean {
    var ex: IllegalStateException? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        ex = e
        caught = true
    } catch (e: Throwable) {
        ex = null
        caught = true
    } finally {
        return caught && ex == null
    }
}

fun customJsErrorWithCatchThrowableAndJsExceptionAndFinally(): Boolean {
    var ex: Throwable? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        ex = e
        caught = true
    } catch (e: JsException) {
        ex = null
        caught = true
    } finally {
        return caught && (ex is JsException) && assertCustomJsError(ex as JsException, "customJsErrorWithCatchThrowableAndJsExceptionAndFinally")
    }
}

fun customJsErrorWithCatchIllegalStateExceptionAndJsExceptionAndFinally(): Boolean {
    var ex: JsException? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: IllegalStateException) {
        ex = null
        caught = true
    } catch (e: JsException) {
        ex = e
        caught = true
    } finally {
        return caught && ex != null && assertCustomJsError(ex!!, "customJsErrorWithCatchIllegalStateExceptionAndJsExceptionAndFinally")
    }
}

fun customJsErrorWithCatchThrowableAndIllegalStateExceptionAndFinally(): Boolean {
    var ex: Throwable? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        ex = e
        caught = true
    } catch (e: IllegalStateException) {
        ex = null
        caught = true
    } finally {
        return caught && (ex is JsException) && assertCustomJsError(ex as JsException, "customJsErrorWithCatchThrowableAndIllegalStateExceptionAndFinally")
    }
}

fun customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsError(e, "customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateException")
    } catch (e: JsException) {
        return false
    } catch (e: IllegalStateException) {
        return false
    }
}

fun customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateExceptionAndFinally(): Boolean {
    var ex: Throwable? = null
    var caught = false
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        ex = e
        caught = true
    } catch (e: JsException) {
        ex = null
        caught = true
    } catch (e: IllegalStateException) {
        ex = null
        caught = true
    } finally {
        return caught && (ex is JsException) && assertCustomJsError(ex as JsException, "customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateExceptionAndFinally")
    }
}

fun box(): String {
    if (!customJsErrorWithFinally()) return "finally only"

    if (!customJsErrorWithCatchThrowable()) return "catch Throwable"
    if (!customJsErrorWithCatchJsException()) return "catch JsException"
    if (!wasThrown<JsException> { customJsErrorWithCatchIllegalStateException() }) return "catch IllegalStateException"

    if (!customJsErrorWithCatchThrowableAndFinally()) return "catch Throwable + finally"
    if (!customJsErrorWithCatchJsExceptionAndFinally()) return "catch JsException + finally"
    if (!customJsErrorWithCatchIllegalStateExceptionAndFinally()) return "catch IllegalStateException + finally"

    if (!customJsErrorWithCatchJsExceptionAndThrowable()) return "catch JsException, Throwable"
    if (!customJsErrorWithCatchIllegalStateExceptionAndThrowable()) return "catch IllegalStateException, Throwable"
    if (!customJsErrorWithCatchThrowableAndJsException()) return "catch Throwable, JsException"
    if (!customJsErrorWithCatchIllegalStateExceptionAndJsException()) return "catch IllegalStateException, JsException"
    if (!customJsErrorWithCatchThrowableAndIllegalStateExceptionAndFinally()) return "catch Throwable, IllegalStateException + finally"

    if (!customJsErrorWithCatchJsExceptionAndThrowableAndFinally()) return "catch JsException, Throwable + finally"
    if (!customJsErrorWithCatchIllegalStateExceptionAndThrowableAndFinally()) return "catch IllegalStateException, Throwable + finally"
    if (!customJsErrorWithCatchThrowableAndJsExceptionAndFinally()) return "catch Throwable, JsException + finally"
    if (!customJsErrorWithCatchIllegalStateExceptionAndJsExceptionAndFinally()) return "catch IllegalStateException, JsException + finally"

    if (!customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateException()) return "catch Throwable, JsException, IllegalStateException"
    if (!customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateExceptionAndFinally()) return "catch Throwable, JsException, IllegalStateException + finally"

    return "OK"
}