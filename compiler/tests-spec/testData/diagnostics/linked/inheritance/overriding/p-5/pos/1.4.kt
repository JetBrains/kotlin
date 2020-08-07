// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 5 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: overriding member custom function of open class
 */

// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase1 : BaseCase1(1, "") {
    override fun foo(): Int = TODO()
}

fun case1(b: BaseCase1, c: ChildCase1) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.foo; typeCall: function")!>foo()<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase1.foo; typeCall: function")!>foo()<!>
}

// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase2 : BaseCase2(1, "") {
    open override fun foo(): Int = TODO()
}


fun case2(b: BaseCase2, c: ChildCase2) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.foo; typeCall: function")!>foo()<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase2.foo; typeCall: function")!>foo()<!>
}


// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun foo(): Int = TODO()
}

fun case3(b: BaseCase3, c: ChildCase3) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.foo; typeCall: function")!>foo()<!>
    c.<!DEBUG_INFO_CALL("fqName: ChildCase3.foo; typeCall: function")!>foo()<!>
}
