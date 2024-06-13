// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/simpleDoWhile.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

inline fun foo(block: () -> Unit) { js("block()") }

fun box(): String {
    var i = 0
    do {
        if (++i == 1)
            foo { continue }

        if (i == 2)
            foo { break }
        return "FAIL 1: $i"
    } while (true)

    if (i != 2) return "FAIL 2: $i"

    return "OK"
}