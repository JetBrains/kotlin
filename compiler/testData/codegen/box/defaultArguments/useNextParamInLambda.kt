// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6

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
