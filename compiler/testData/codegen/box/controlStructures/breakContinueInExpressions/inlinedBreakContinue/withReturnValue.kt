// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported

// FILE: lib.kt
inline fun foo(block: () -> Int): Int  = block()

// FILE: main.kt
fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL: $sum"
}