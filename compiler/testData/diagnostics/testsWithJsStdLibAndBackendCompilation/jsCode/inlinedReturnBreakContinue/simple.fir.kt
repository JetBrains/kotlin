// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/simple.kt
// LANGUAGE: +BreakContinueInInlineLambdas

inline fun foo(block: () -> Unit) { js("block()") }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}
