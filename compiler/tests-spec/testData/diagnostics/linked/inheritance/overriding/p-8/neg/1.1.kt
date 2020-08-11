// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: inheritance, overriding -> paragraph 8 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * SECONDARY LINKS: inheritance, overriding -> paragraph 2 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 2
 * inheritance, overriding -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: overriding member toString function of open class (Return type of a child override function is not a subtype of return type of a base class)
 */

// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence)

open class ChildCase1 : BaseCase1(1, "") {
    override fun toString(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>CharSequence<!> = TODO() //(1)
}

// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open override fun toString(): String = TODO() //(0)
}

open class ChildCase2 : BaseCase2(1, "") {
    open override fun toString(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>CharSequence<!> = TODO() //(1)
}

// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    override fun toString(): String = TODO() //(0)
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun toString(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>CharSequence<!> = TODO() //(1)
}
