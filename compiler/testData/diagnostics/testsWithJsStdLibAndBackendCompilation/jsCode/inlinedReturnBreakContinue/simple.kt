// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +BreakContinueInInlineLambdas +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/simple.kt

inline fun foo(block: () -> Unit) { js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_ERROR!>"block()"<!>) }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}
