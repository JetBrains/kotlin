// IGNORE_BACKEND: JS, JS_IR, WASM
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_K2: JVM_IR
// WASM_MUTE_REASON: IGNORED_IN_JS
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
