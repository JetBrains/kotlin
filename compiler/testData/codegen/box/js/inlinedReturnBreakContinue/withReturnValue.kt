// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/withReturnValue.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

inline fun foo(block: () -> Int): Int  = js("block()")

fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL: $sum"
}