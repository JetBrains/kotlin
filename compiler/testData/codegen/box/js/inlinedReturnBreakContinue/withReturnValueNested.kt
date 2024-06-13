// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/withReturnValueNested.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label '$l$loop'

inline fun foo(block: () -> Int): Int  = js("block()")

fun box(): String {
    val resultNonLabeled = testNonLabeledBreaks()
    if (resultNonLabeled != (1)+(1+2)+(1+2)+(1+2)) return "FAIL testNonLabeledBreaks: $resultNonLabeled"

    val resultLabeled = testLabeledBreaks()
    if (resultLabeled != (1)+(1+2)) return "FAIL testLabeledBreaks: $resultLabeled"

    val resultForEach = testForEach()
    if (resultForEach != (1)+(1+2)) return "FAIL testForEach: $resultForEach"
    return "OK"
}

private fun testNonLabeledBreaks(): Int {
    var sum = 0

    for (i in 1..10) {
        sum += foo {
            if (i == 5) break else {
                var innerSum = 0
                for (j in 1..i) {
                    innerSum += foo {
                        if (j == 3) break else j
                    }
                }
                innerSum
            }
        }
    }
    return sum
}

private fun testLabeledBreaks(): Int {
    var sum = 0

    outer@
    for (i in 1..10) {
        sum += foo {
            if (i == 5) break else {
                var innerSum = 0
                for (j in 1..i) {
                    innerSum += foo {
                        if (j == 3) break@outer else j
                    }
                }
                innerSum
            }
        }
    }
    return sum
}

private fun testForEach(): Int {
    var sum = 0

    for (i in 1..10) {
        sum += foo {
            if (i == 5) break else {
                var innerSum = 0
                (1..i).forEach { j: Int ->
                    innerSum += foo {
                        if (j == 3) break else j // breaks not `forEach`, but outer `for`, so similar to `break@outer`
                    }
                }
                innerSum
            }
        }
    }
    return sum
}
