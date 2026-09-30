// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

annotation class Anno(val i: Int)

class Outer {
    @Anno(i = fun foo() = 1)
    class Nested

    @Anno(i = fun foo() = 1)
    inner class Inner

    @Anno(i = fun foo() = 1)
    object Obj
}
