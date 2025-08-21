// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported

// FILE: lib.kt
inline fun foo(block: () -> Unit) { block() }

// FILE: main.kt
fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}