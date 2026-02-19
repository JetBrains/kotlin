// TARGET_BACKEND: WASM
// ^ The test is specific for Wasm envorinment without JSTag. It's not relevant for JS_IR and JS_IR_ES6
// WASM_NO_JS_TAG

fun throwCustomNamedJsError(): Int = js("""
{
    const e = new Error("Custom error message");
    e.name = "MyCustomError";
    throw e;
}
""")

inline fun <reified T : Throwable> wasThrown(fn: () -> Any?): Boolean {
    try {
        fn()
        return false
    } catch (e: Throwable) {
        return e is T
    }
}

private fun assertCustomJsErrorNoJsTag(e: JsException): Boolean =
    e.message == "Exception was thrown while running JavaScript code" &&
            e.thrownValue == null

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
        return e is JsException && assertCustomJsErrorNoJsTag(e)
    }
}

fun customJsErrorWithCatchJsException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: JsException) {
        return assertCustomJsErrorNoJsTag(e)
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
        return ex is JsException && assertCustomJsErrorNoJsTag(ex as JsException)
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
        return ex != null && assertCustomJsErrorNoJsTag(ex!!)
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
        return assertCustomJsErrorNoJsTag(e)
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
        return e is JsException && assertCustomJsErrorNoJsTag(e)
    }
}

fun customJsErrorWithCatchThrowableAndJsException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsErrorNoJsTag(e)
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
        return assertCustomJsErrorNoJsTag(e)
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
        return caught && ex != null && assertCustomJsErrorNoJsTag(ex!!)
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
        return caught && (ex is JsException) && assertCustomJsErrorNoJsTag(ex as JsException)
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
        return caught && ex != null && assertCustomJsErrorNoJsTag(ex!!)
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
        return caught && (ex is JsException) && assertCustomJsErrorNoJsTag(ex as JsException)
    }
}

fun customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateException(): Boolean {
    try {
        throwCustomNamedJsError()
        return false
    } catch (e: Throwable) {
        return e is JsException && assertCustomJsErrorNoJsTag(e)
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
        return caught && (ex is JsException) && assertCustomJsErrorNoJsTag(ex as JsException)
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

    if (!customJsErrorWithCatchJsExceptionAndThrowableAndFinally()) return "catch JsException, Throwable + finally"
    if (!customJsErrorWithCatchIllegalStateExceptionAndThrowableAndFinally()) return "catch IllegalStateException, Throwable + finally"
    if (!customJsErrorWithCatchThrowableAndJsExceptionAndFinally()) return "catch Throwable, JsException + finally"
    if (!customJsErrorWithCatchIllegalStateExceptionAndJsExceptionAndFinally()) return "catch IllegalStateException, JsException + finally"
    if (!customJsErrorWithCatchThrowableAndIllegalStateExceptionAndFinally()) return "catch Throwable, IllegalStateException + finally"

    if (!customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateException()) return "catch Throwable, JsException, IllegalStateException"
    if (!customJsErrorWithCatchThrowableAndJsExceptionAndIllegalStateExceptionAndFinally()) return "catch Throwable, JsException, IllegalStateException + finally"

    return "OK"
}