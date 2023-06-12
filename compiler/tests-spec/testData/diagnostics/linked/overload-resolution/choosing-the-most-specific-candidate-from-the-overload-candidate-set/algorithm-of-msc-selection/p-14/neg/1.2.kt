// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 14 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: an overload ambiguity which must be reported if several candidates which are equally applicable for the call exests
 */

// TESTCASE NUMBER: 6
// NOTE: todo link new sentences
class A : B, C
interface B
interface C
fun foo(x: B) {} //(1)
fun foo(y: C, z: String = "foo") {} //2
fun bar() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(A()) //OVERLOAD_RESOLUTION_AMBIGUITY
}
