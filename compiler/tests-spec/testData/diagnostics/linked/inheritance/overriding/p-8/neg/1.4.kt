// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: inheritance, overriding -> paragraph 8 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * SECONDARY LINKS: inheritance, overriding -> paragraph 2 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 2
 * inheritance, overriding -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: overriding member custom function of open class (Return type of a child override function is not a subtype of return type of a base class)
 */

// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase1a : BaseCase1(1, "") {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = 2.0
}

open class ChildCase1b : BaseCase1(1, "") {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Any<!> = TODO()
}

// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open fun foo(): Double = TODO()
}

open class ChildCase2 : BaseCase2(1, "") {
    override open fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Int<!> = TODO()
}

// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    open fun foo(): Int = TODO()
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = TODO()
}