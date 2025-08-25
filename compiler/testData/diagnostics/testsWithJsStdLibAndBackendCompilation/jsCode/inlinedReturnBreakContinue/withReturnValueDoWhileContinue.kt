// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +BreakContinueInInlineLambdas +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/withReturnValueDoWhileContinue.kt

inline fun foo(block: () -> Int): Int  = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block()"<!>)

fun box(): String {
    var sum = 0
    var i = 1
    do {
        sum += foo { if (i == 3) continue else i }
    } while (++i <= 5)

    return if (sum == 1 + 2 + 4 + 5) "OK" else "FAIL: $sum"
}
