// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: inheritance, overriding -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: overriding extension property of open class
 */

// TESTCASE NUMBER: 1
open class BaseCase1()

val BaseCase1.foo: Int
    get() = 1

val BaseCase1.boo: Int
    get() = 1

open class ChildCase1 : BaseCase1() {
    <!NOTHING_TO_OVERRIDE!>override<!> val foo: Int = TODO()
    <!NOTHING_TO_OVERRIDE!>override<!> val boo: Int = TODO()
}

fun case1(b: BaseCase1, c: ChildCase1) {
    b.<!DEBUG_INFO_CALL("fqName: foo; typeCall: variable")!>foo<!>
    b.<!DEBUG_INFO_CALL("fqName: boo; typeCall: variable")!>boo<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase1.foo; typeCall: variable")!>foo<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase1.boo; typeCall: variable")!>boo<!>
}

// TESTCASE NUMBER: 2
open class BaseCase2()

var BaseCase2.foo: Int
    get() = 1
    set(value) {}

var BaseCase2.boo: Int
    get() = 1
    set(value) {}

open class ChildCase2 : BaseCase2() {
    <!NOTHING_TO_OVERRIDE!>override<!> var foo: Int = TODO()
}

fun case2(b: BaseCase2, c: ChildCase2) {
    b.<!DEBUG_INFO_CALL("fqName: foo; typeCall: variable")!>foo<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase2.foo; typeCall: variable")!>foo<!>
}