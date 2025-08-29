// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL

private val TEST_JS_STRING = "Test".toJsString()
private val JS_42 = 42.toJsNumber()

fun throwJsPrimitive(): Int = js("{ throw 'Test'; }")
fun throwJsNumber(): Int = js("{ throw 42; }")
fun throwJsNull(): Int = js("{ throw null; }")
fun throwJsTypeError(): Int = js("{ throw new TypeError('Test'); }")

@JsName("TypeError")
external class JsTypeError : JsAny

fun throwJsValueBack(v: JsAny?): Nothing = js("{ throw v; }")

private inline fun <reified T : Throwable> wasThrown(fn: () -> Any?): Boolean =
    try { fn(); false } catch (e: Throwable) { e is T }

private fun String.contains(module: String, funName: String) =
    contains("$module.$funName") || contains("$funName@$module")

fun rtString_viaThrowable(): Boolean {
    try {
        try {
            throwJsPrimitive()
        } catch (e: Throwable) {
            if (e !is JsException) return false
            if (e.message != "Exception was thrown while running JavaScript code") return false
            if (e.thrownValue != TEST_JS_STRING) return false
            throwJsValueBack(e.thrownValue)
        }
        return false
    } catch (e2: Throwable) {
        return e2 is JsException &&
                e2.message == "Exception was thrown while running JavaScript code" &&
                e2.thrownValue == TEST_JS_STRING
    }
}

fun rtNumber_viaThrowable(): Boolean {
    try {
        try {
            throwJsNumber()
        } catch (e: Throwable) {
            if (e !is JsException) return false
            if (e.thrownValue != JS_42) return false
            throwJsValueBack(e.thrownValue)
        }
        return false
    } catch (e2: Throwable) {
        return e2 is JsException &&
                e2.message == "Exception was thrown while running JavaScript code" &&
                e2.thrownValue == JS_42
    }
}

fun rtNull_viaThrowable(): Boolean {
    try {
        try {
            throwJsNull()
        } catch (e: Throwable) {
            if (e !is JsException) return false
            if (e.thrownValue != null) return false
            throwJsValueBack(e.thrownValue)
        }
        return false
    } catch (e2: Throwable) {
        return e2 is JsException &&
                e2.message == "Exception was thrown while running JavaScript code" &&
                e2.thrownValue == null
    }
}

fun rtTypeError_viaJsException(): Boolean {
    try {
        try {
            throwJsTypeError()
        } catch (e: JsException) {
            if (e.message != "Test") return false
            if (e.thrownValue !is JsTypeError) return false
            throwJsValueBack(e.thrownValue)
        }
        return false
    } catch (e2: Throwable) {
        val st = e2.stackTraceToString()
        return e2 is JsException &&
                e2.message == "Test" &&
                e2.thrownValue is JsTypeError &&
                st.contains("throwJsTypeError") &&
                st.contains("<main>", "rtTypeError_viaJsException") &&
                st.contains("<main>", "box")
    }
}

fun rtString_withFinally(): Boolean {
    var finallyRan = false
    try {
        try {
            throwJsPrimitive()
        } catch (e: Throwable) {
            if (e !is JsException) return false
            throwJsValueBack(e.thrownValue)
        } finally {
            finallyRan = true
        }
        return false
    } catch (e2: Throwable) {
        return finallyRan &&
                e2 is JsException &&
                e2.message == "Exception was thrown while running JavaScript code" &&
                e2.thrownValue == TEST_JS_STRING
    }
}

fun rtTypeError_withFinally(): Boolean {
    var finallyRan = false
    try {
        try {
            throwJsTypeError()
        } catch (e: JsException) {
            if (e.thrownValue !is JsTypeError) return false
            throwJsValueBack(e.thrownValue)
        } finally {
            finallyRan = true
        }
        return false
    } catch (e2: Throwable) {
        val st = e2.stackTraceToString()
        return finallyRan &&
                e2 is JsException &&
                e2.message == "Test" &&
                e2.thrownValue is JsTypeError &&
                st.contains("throwJsTypeError") &&
                st.contains("<main>", "rtTypeError_withFinally") &&
                st.contains("<main>", "box")
    }
}

fun rtNotCaughtAsIllegalState(): Boolean = wasThrown<JsException> {
    try {
        throwJsPrimitive()
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: Throwable) {
        if (e !is JsException) return@wasThrown false
        throwJsValueBack(e.thrownValue)
    }
}

fun box(): String {
    if (!rtString_viaThrowable()) return "rtString_viaThrowable failed"
    if (!rtNumber_viaThrowable()) return "rtNumber_viaThrowable failed"
    if (!rtNull_viaThrowable()) return "rtNull_viaThrowable failed"
    if (!rtTypeError_viaJsException()) return "rtTypeError_viaJsException failed"

    if (!rtString_withFinally()) return "rtString_withFinally failed"
    if (!rtTypeError_withFinally()) return "rtTypeError_withFinally failed"

    if (!rtNotCaughtAsIllegalState()) return "rtNotCaughtAsIllegalState failed"

    return "OK"
}
