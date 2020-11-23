// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun fn0() {}
fun fn1(x: Any) {}

inline fun <reified T> reifiedAsSucceeds(x: Any, operation: String) {
    try {
        x as T
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

inline fun <reified T> reifiedAsFailsWithCCE(x: Any, operation: String) {
    try {
        x as T
    }
    catch (e: ClassCastException) {
        return
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should throw ClassCastException, got $e")
    }
    throw AssertionError("$operation: should fail with CCE, no exception thrown")
}

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    reifiedAsSucceeds<Function0<*>>(f0, "f0 as Function0<*>")
    reifiedAsFailsWithCCE<Function1<*, *>>(f0, "f0 as Function1<*, *>")
    reifiedAsFailsWithCCE<Function0<*>>(f1, "f1 as Function0<*>")
    reifiedAsSucceeds<Function1<*, *>>(f1, "f1 as Function1<*, *>")

    return "OK"
}
