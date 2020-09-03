// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, data-flow-framework, smart-cast-transfer-functions -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, cast-expressions -> paragraph 3 -> sentence 1
 * expressions, type-checking-and-containment-checking-expressions, type-checking-expressions -> paragraph 6 -> sentence 1
 * SECONDARY LINKS: expressions, type-checking-and-containment-checking-expressions, type-checking-expressions -> paragraph 8 -> sentence 1
 * type-inference, smart-casts -> paragraph 1 -> sentence 2
 * type-inference, smart-casts -> paragraph 1 -> sentence 1
 * type-inference, smart-casts -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: as + is
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41575
 */

fun case1(x: Any) {
    if ((x as CharSequence) is String) {
       <!DEBUG_INFO_SMARTCAST!>x<!>.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo()<!> // to (2)
       <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!DEBUG_INFO_SMARTCAST!>x<!>.foo()<!>
    }
}
fun String.foo() {} //(1)
fun CharSequence.foo() : Int = 2 //(2)