// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/simple.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

inline fun foo(block: () -> Unit) { js("block()") }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}