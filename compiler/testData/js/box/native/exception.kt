// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
external class TypeError(message: String?, fileName: String? = definedExternally, lineNumber: Int? = definedExternally) : Throwable

fun box(): String {
    try {
        js("null.foo()")
        return "fail: expected exception not thrown"
    }
    catch (e: TypeError) {
        return "OK"
    }
}