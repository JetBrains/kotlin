// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM

inline fun foo(block: () -> Int): Int  = block()

fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL"
}