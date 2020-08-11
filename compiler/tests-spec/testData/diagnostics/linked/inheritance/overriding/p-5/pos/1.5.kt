// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: inheritance, overriding -> paragraph 5 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: overriding member custom properties of open class
 */
// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    override val a1: Int = 1
    override var a2: Int = 1
    override val b1: Int = 1
    override var b2: Int = 1
}

open class Case1b() : BaseCase1(1, 1) {
    override val a1 = 1
    override var a2 = 1
    override val b1 = 1
    override var b2 = 1
}

fun case1a(b: BaseCase1, ca: Case1a, cb: Case1b) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.a1; typeCall: variable")!>a1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.a2; typeCall: variable")!>a2<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.b1; typeCall: variable")!>b1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase1.b2; typeCall: variable")!>b2<!>

    ca.<!DEBUG_INFO_CALL("fqName: Case1a.a1; typeCall: variable")!>a1<!>
    ca.<!DEBUG_INFO_CALL("fqName: Case1a.a2; typeCall: variable")!>a2<!>
    ca.<!DEBUG_INFO_CALL("fqName: Case1a.b1; typeCall: variable")!>b1<!>
    ca.<!DEBUG_INFO_CALL("fqName: Case1a.b2; typeCall: variable")!>b2<!>

    cb.<!DEBUG_INFO_CALL("fqName: Case1b.a1; typeCall: variable")!>a1<!>
    cb.<!DEBUG_INFO_CALL("fqName: Case1b.a2; typeCall: variable")!>a2<!>
    cb.<!DEBUG_INFO_CALL("fqName: Case1b.b1; typeCall: variable")!>b1<!>
    cb.<!DEBUG_INFO_CALL("fqName: Case1b.b2; typeCall: variable")!>b2<!>
}


// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    override val a1: Int
        get() {
            return 1
        }
    override var a2: Int
        get() {
            return 1
        }
        set(value) {}
    override val b1: Int
        get() {
            return 1
        }
    override var b2: Int
        get() {
            return 1
        }
        set(value) {}
}

fun case2(b: BaseCase2, c: Case2) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.a1; typeCall: variable")!>a1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.a2; typeCall: variable")!>a2<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.b1; typeCall: variable")!>b1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase2.b2; typeCall: variable")!>b2<!>

    c.<!DEBUG_INFO_CALL("fqName: Case2.a1; typeCall: variable")!>a1<!>
    c.<!DEBUG_INFO_CALL("fqName: Case2.a2; typeCall: variable")!>a2<!>
    c.<!DEBUG_INFO_CALL("fqName: Case2.b1; typeCall: variable")!>b1<!>
    c.<!DEBUG_INFO_CALL("fqName: Case2.b2; typeCall: variable")!>b2<!>
}

// TESTCASE NUMBER: 3
open class BaseCase3(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case3() : BaseCase3(1, 1) {
    override val a1: Int
        get() = 1
    override var a2: Int
        get() = 1
        set(value) {}
    override val b1: Int
        get() = 1
    override var b2: Int
        get() = 1
        set(value) {}
}

fun case3(b: BaseCase3, c: Case3) {
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.a1; typeCall: variable")!>a1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.a2; typeCall: variable")!>a2<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.b1; typeCall: variable")!>b1<!>
    b.<!DEBUG_INFO_CALL("fqName: BaseCase3.b2; typeCall: variable")!>b2<!>

    c.<!DEBUG_INFO_CALL("fqName: Case3.a1; typeCall: variable")!>a1<!>
    c.<!DEBUG_INFO_CALL("fqName: Case3.a2; typeCall: variable")!>a2<!>
    c.<!DEBUG_INFO_CALL("fqName: Case3.b1; typeCall: variable")!>b1<!>
    c.<!DEBUG_INFO_CALL("fqName: Case3.b2; typeCall: variable")!>b2<!>
}