// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +BreakContinueInInlineLambdas +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/inlineFunctionWithMultipleParameters.kt

import kotlin.test.assertEquals

inline fun foo(
    block1: () -> Unit,
    noinline block2: () -> Unit,
    block3: () -> Unit
) {
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block1()"<!>)
    js("block2()")
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block3()"<!>)
}

fun box(): String {
    val visited = mutableListOf<Int>()

    for (i in 1..3) {
        foo(
            { visited += 1; if (i == 1) continue },
            { visited += 2 },
            { visited += 3; if (i == 2) break },
        )
    }

    assertEquals(listOf(1, 1, 2, 3), visited)

    return "OK"
}