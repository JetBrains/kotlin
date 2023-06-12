// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-401
 * MAIN LINK: overload-resolution, resolving-callable-references -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: the case of a call with a callable reference as a not parameter
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

class Case1() {
    fun foo(x: A): A = x//(1)
}

fun Case1.foo(i: B): B = i //(2)

class A : C
class B : C
interface C

fun <T : C> process(call: (T) -> T, x: T): T = call.invoke(x)

fun case1() {
    val case = Case1()

    <!DEBUG_INFO_EXPRESSION_TYPE("testsCase1.A")!>process(case::foo, A())<!>
    process(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<testsCase1.A, testsCase1.A>")!>case::foo<!>, A())
    process(case:: foo, A())

    <!DEBUG_INFO_EXPRESSION_TYPE("testsCase1.B")!>process(case::foo, B())<!>
    process(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<testsCase1.B, testsCase1.B>")!>case::foo<!>, B())
    process(case:: foo, B())

}
