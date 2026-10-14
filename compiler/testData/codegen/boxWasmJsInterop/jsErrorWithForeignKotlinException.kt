// TARGET_BACKEND: WASM
// WITH_STDLIB
// A Kotlin exception which crosses JS is recovered through the `kotlinException` property of its JS error.
// That property is plain JS, so it may refer to something else than a Throwable: then the JS error is wrapped in a JsException.
@file:OptIn(ExperimentalWasmJsInterop::class)

class NotAThrowable

fun throwJsErrorWithKotlinException(ref: JsReference<Any>): Int =
    js("{ const e = new Error('from JS'); e.kotlinException = ref; throw e; }")

fun callFromJs(f: () -> Unit): Int = js("{ f(); return 0; }")

fun box(): String {
    val kotlinException = IllegalStateException("from Kotlin")
    try {
        callFromJs { throw kotlinException }
        return "FAIL: no exception from Kotlin"
    } catch (e: Throwable) {
        if (e !== kotlinException) return "FAIL: got $e instead of the original Kotlin exception"
    }

    try {
        throwJsErrorWithKotlinException(NotAThrowable().toJsReference())
        return "FAIL: no exception from JS"
    } catch (e: JsException) {
        if (e.message != "from JS") return "FAIL: message ${e.message}"
    } catch (e: Throwable) {
        return "FAIL: got $e instead of a JsException"
    }

    return "OK"
}
