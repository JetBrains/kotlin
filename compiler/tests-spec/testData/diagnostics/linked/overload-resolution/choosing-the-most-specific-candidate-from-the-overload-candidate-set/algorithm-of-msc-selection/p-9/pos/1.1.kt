// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK:overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 9 -> sentence 1
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Neither of the two candidates is more applicable than the other and one is non-parameterized
 */
// TESTCASE NUMBER: 1
class A : B, C
interface B
interface C
fun <T>foo(x: B) {} //(1)
fun foo(y: C, z: String = "foo") : String = "" //2
fun case1(a: A) {
    <!DEBUG_INFO_CALL("fqName: foo; typeCall: function")!>foo(a)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(a)<!>
}

// TESTCASE NUMBER: 2
class A2 : B2, C2
interface B2
interface C2
fun boo(x: B2) ="" //(1)
fun <T>boo(y: C, z: String = "boo") {} //2
fun case2(a: A2) {
    <!DEBUG_INFO_CALL("fqName: boo; typeCall: function")!>boo(a)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(a)<!>
}
