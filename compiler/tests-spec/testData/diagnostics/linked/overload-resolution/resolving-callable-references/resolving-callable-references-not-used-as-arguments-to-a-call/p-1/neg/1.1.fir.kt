// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-413
 * MAIN LINK: overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 5 -> sentence 1
 * SECONDARY LINKS: overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: the case of a call with a callable reference as a not parameter
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import testsCase1.Case1.Companion.foo

class Case1() {
    companion object {
        fun foo() : Case1 = TODO()
        fun foo(y: String, x: Any = "") : Case1 = TODO()
        fun foo( x: String, y: String) : Case1 = TODO()
    }

}
fun case1() {
    val y0: (String)-> Case1 = ::foo
    val y1: (String)-> Case1 = Case1.Companion::foo
    val y2: (String)-> Case1 = (Case1)::foo
}

fun case1_0() : (String)-> Case1 = ::foo
fun case1_1() : (String)-> Case1 = (Case1)::foo
fun case1_2(): (String)-> Case1 = Case1.Companion::foo
