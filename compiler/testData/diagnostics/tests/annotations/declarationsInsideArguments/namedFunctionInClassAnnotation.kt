// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

package testPack

annotation class Anno(val i: Int)

@Anno(i = fun foo() = 1)
abstract class Check {
    abstract var prop: Int
}
