// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/withReturnValueDoWhileContinue.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

inline fun foo(block: () -> Int): Int  = js("block()")

fun box(): String {
    var sum = 0
    var i = 1
    do {
        sum += foo { if (i == 3) continue else i }
    } while (++i <= 5)

    return if (sum == 1 + 2 + 4 + 5) "OK" else "FAIL: $sum"
}