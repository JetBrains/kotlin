// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, the test doesn't work since we don't expect that non Error could came to the catch block

private val TEST_JS_STRING = "Test".toJsString()
private val JS_42 = 42.toJsNumber()

fun throwJsPrimitive(): Int = js("{ throw 'Test'; }")
fun throwJsNumber(): Int = js("{ throw 42; }")
fun throwJsNull(): Int = js("{ throw null; }")
fun throwJsTypeError(): Int = js("{ throw new TypeError('Test'); }")

fun throwJsValueBack(v: JsAny?): Nothing = js("{ throw v; }")

fun rtString(): Boolean {
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

fun rtNumber(): Boolean {
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

fun rtNull(): Boolean {
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

fun jsCtorName(v: JsAny?): String? = js("(v && v.constructor && v.constructor.name) || null")

fun rtTypeError(): Boolean {
    try {
        try {
            throwJsTypeError()
        } catch (e: Throwable) {
            if (e !is JsException) return false
            if (e.message != "Test") return false
            if (jsCtorName(e.thrownValue) != "TypeError") return false
            throwJsValueBack(e.thrownValue)
        }
        return false
    } catch (e2: Throwable) {
        return e2 is JsException &&
                e2.message == "Test" &&
                jsCtorName(e2.thrownValue) == "TypeError"
    }
}

fun box(): String {
    if (!rtString()) return "rtString failed"
    if (!rtNumber()) return "rtNumber failed"
    if (!rtNull()) return "rtNull failed"
    if (!rtTypeError()) return "rtTypeError failed"
    return "OK"
}
