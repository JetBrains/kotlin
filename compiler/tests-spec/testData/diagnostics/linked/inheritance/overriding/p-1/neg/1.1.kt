// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: overriding extension function of open class
 */

// TESTCASE NUMBER: 1
open class BaseCase1()

fun BaseCase1.foo(): Int = TODO()

open class ChildCase1 : BaseCase1() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(): Int = TODO()
}

fun case1(b: BaseCase1, c: ChildCase1) {
    b.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo()<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase1.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 2
open class BaseCase2()

fun BaseCase2.foo(): Int = TODO()


open class ChildCase2 : BaseCase2() {
    open <!NOTHING_TO_OVERRIDE!>override<!> fun foo(): Int = TODO()
}

fun case2(b: BaseCase2, c: ChildCase2) {
    b.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo()<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase2.foo; typeCall: function")!>foo()<!>
}