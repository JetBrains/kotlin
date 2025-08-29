// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: WASM

import kotlin.coroutines.*
import helpers.*

fun thenPromise(
    p: JsAny?,
    ok: (JsAny?) -> Unit,
    err: (JsAny?) -> Unit
): Unit = js("{ p.then(ok, err); }")

fun throwJsValueBack(v: JsAny?): Nothing = js("{ throw v; }")

fun promiseRejectString(): JsAny? = js("{ return Promise.reject('Test'); }")

fun promiseRejectNumber(): JsAny? = js("{ return Promise.reject(42); }")

fun promiseRejectTypeError(): JsAny? = js("{ return Promise.reject(new TypeError('Test')); }")

fun jsCtorName(v: JsAny?): String? = js("(v && v.constructor && v.constructor.name) || null")

private fun jsValueToThrowable(v: JsAny?): Throwable =
    try { throwJsValueBack(v) } catch (t: Throwable) { t }

suspend fun awaitJs(p: JsAny?): JsAny? =
    suspendCoroutine { cont ->
        thenPromise(
            p,
            { v -> cont.resume(v) },
            { e -> cont.resumeWithException(jsValueToThrowable(e)) }
        )
    }

private val TEST_JS_STRING = "Test".toJsString()
private val JS_42 = 42.toJsNumber()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun reject_string_caught_as_JsException(): Boolean {
    try {
        awaitJs(promiseRejectString())
        return false
    } catch (e: Throwable) {
        return e is JsException &&
                e.message == "Exception was thrown while running JavaScript code" &&
                e.thrownValue == TEST_JS_STRING
    }
}

suspend fun reject_number_caught_as_JsException(): Boolean {
    try {
        awaitJs(promiseRejectNumber())
        return false
    } catch (e: Throwable) {
        return e is JsException &&
                e.message == "Exception was thrown while running JavaScript code" &&
                e.thrownValue == JS_42
    }
}

suspend fun reject_typeerror_preserves_message_and_type(): Boolean {
    try {
        awaitJs(promiseRejectTypeError())
        return false
    } catch (e: JsException) {
        return e.message == "Test" && jsCtorName(e.thrownValue) == "TypeError"
    }
}

fun box(): String {
    var result = "OK"
    builder {
        if (!reject_string_caught_as_JsException()) result = "reject string failed"
        else if (!reject_number_caught_as_JsException()) result = "reject number failed"
        else if (!reject_typeerror_preserves_message_and_type()) result = "reject TypeError failed"
    }
    return result
}
