// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/inlineFunctionWithMultipleParameters.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

import kotlin.test.assertEquals

inline fun foo(
    block1: () -> Unit,
    noinline block2: () -> Unit,
    block3: () -> Unit
) {
    js("block1()")
    js("block2()")
    js("block3()")
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