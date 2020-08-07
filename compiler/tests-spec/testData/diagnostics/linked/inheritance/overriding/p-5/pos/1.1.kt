// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: overriding member toString function of open class
 */

// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence)

open class ChildCase1 : BaseCase1(1, "") {
    override fun toString(): String = TODO() //(1)
}

fun case1(c: ChildCase1, b:BaseCase1) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.toString; typeCall: function")!>toString()<!> //to generated fun

    c.<!DEBUG_INFO_CALL("fqName: ChildCase1.toString; typeCall: function")!>toString()<!> //to (1)
}

// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open override fun toString(): String = TODO() //(0)
}

open class ChildCase2 : BaseCase2(1, "") {
    open override fun toString(): String = TODO() //(1)
}


fun case2(c: ChildCase2, b:BaseCase2) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.toString; typeCall: function")!>toString()<!> //to (0)

    c.<!DEBUG_INFO_CALL("fqName: ChildCase2.toString; typeCall: function")!>toString()<!> //to (1)
}

// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    override fun toString(): String = TODO() //(0)
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun toString(): String = TODO() //(1)
}

fun case3(c: ChildCase3, b:BaseCase3) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.toString; typeCall: function")!>toString()<!> //to (0)

    c.<!DEBUG_INFO_CALL("fqName: ChildCase3.toString; typeCall: function")!>toString()<!> //to (1)
}