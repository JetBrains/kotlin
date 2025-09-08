// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +BreakContinueInInlineLambdas +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/simpleDoWhile.kt

inline fun foo(block: () -> Unit) { js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block()"<!>) }

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
