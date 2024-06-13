// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/withReturnValue.kt
// LANGUAGE: +BreakContinueInInlineLambdas

inline fun foo(<!UNUSED_PARAMETER!>block<!>: () -> Int): Int  = js("block()")

fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL: $sum"
}
