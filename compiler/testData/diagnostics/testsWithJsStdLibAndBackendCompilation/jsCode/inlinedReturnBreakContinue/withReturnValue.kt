// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +BreakContinueInInlineLambdas +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/withReturnValue.kt

inline fun foo(block: () -> Int): Int  = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block()"<!>)

fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL: $sum"
}
