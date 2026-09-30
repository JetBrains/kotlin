// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations
@file:Anno(i = fun foo() = 1)

package testPack

@Target(AnnotationTarget.FILE)
annotation class Anno(val i: Int)
