// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/simple.kt

inline fun foo(block: () -> Unit) { js("block()") }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}
