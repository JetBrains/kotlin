// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 8 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * SECONDARY LINKS: inheritance, overriding -> paragraph 2 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 2
 * inheritance, overriding -> paragraph 4 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: overriding member custom properties of open class (Return type of a child override function is not a subtype of return type of a base class)
 */

// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    override val a1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!> = 1
    override var a2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!> = 1
    override val b1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!> = 1
    override var b2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!> = 1
}

open class Case1b() : BaseCase1(1, 1) {
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>a1<!> = "1"
    override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>a2<!> = "1"
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>b1<!> = "1"
    override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>b2<!> = "1"
}

// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    override val a1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() {
            return 1
        }
    override var a2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() {
            return 1
        }
        set(value) {}
    override val b1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() {
            return 1
        }
    override var b2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
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
    override val a1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() = 1
    override var a2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() = 1
        set(value) {}
    override val b1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() = 1
    override var b2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
        get() = 1
        set(value) {}
}
