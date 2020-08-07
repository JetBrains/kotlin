// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 9 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 2
 * inheritance, overriding -> paragraph 4 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: overriding member custom properties of open class
 */

// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    val <!VIRTUAL_MEMBER_HIDDEN!>a1<!>: Int = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>a2<!>: Int = 1
    val <!VIRTUAL_MEMBER_HIDDEN!>b1<!>: Int = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>b2<!>: Int = 1
}

open class Case1b() : BaseCase1(1, 1) {
    val <!VIRTUAL_MEMBER_HIDDEN!>a1<!> = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>a2<!> = 1
    val <!VIRTUAL_MEMBER_HIDDEN!>b1<!> = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>b2<!> = 1
}

// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    val <!VIRTUAL_MEMBER_HIDDEN!>a1<!>: Int
        get() {
            return 1
        }
    var <!VIRTUAL_MEMBER_HIDDEN!>a2<!>: Int
        get() {
            return 1
        }
        set(value) {}
    val <!VIRTUAL_MEMBER_HIDDEN!>b1<!>: Int
        get() {
            return 1
        }
    var <!VIRTUAL_MEMBER_HIDDEN!>b2<!>: Int
        get() {
            return 1
        }
        set(value) {}
}

// TESTCASE NUMBER: 3
open class BaseCase3(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case3() : BaseCase3(1, 1) {
    val <!VIRTUAL_MEMBER_HIDDEN!>a1<!>: Int
        get() = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>a2<!>: Int
        get() = 1
        set(value) {}
    val <!VIRTUAL_MEMBER_HIDDEN!>b1<!>: Int
        get() = 1
    var <!VIRTUAL_MEMBER_HIDDEN!>b2<!>: Int
        get() = 1
        set(value) {}
}