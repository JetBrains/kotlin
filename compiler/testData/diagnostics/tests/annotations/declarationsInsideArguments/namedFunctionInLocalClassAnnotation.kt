// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041

annotation class Anno(val i: Int)

fun test() {
    @Anno(i = fun foo() = 1)
    class Local

    val obj = @Anno(i = fun foo() = 1) object {}
}
