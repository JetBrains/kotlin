// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM

inline fun foo(block: () -> Unit) { block() }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}