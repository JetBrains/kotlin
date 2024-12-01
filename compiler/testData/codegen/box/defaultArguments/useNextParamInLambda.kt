// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM
// ^^^ TypeError: $f2 is not a function
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: NATIVE
// ^^^ Exit code is 139 while 0 was expected.
// FIR status: don't support legacy feature. UNINITIALIZED_PARAMETER y. See KT-49800
// LANGUAGE: -ProhibitIllegalValueParameterUsageInDefaultArguments

fun f(
    f1: () -> String = { f2() },
    f2: () -> String = { "Fail: should not be called" }
): String = f1()

fun box(): String {
    try {
        f()
        return "Fail: f() should have thrown NPE"
    } catch (e : Exception) {
    }
    return f(f2 = { "O" }) + f(f1 = { "K" })
}
