// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check as the result of dec is assigned to A, the return type of inc must be a subtype of A.
 */

// TESTCASE NUMBER: 1

fun case1() {
    var a = Case1()
    val res: Any? = <!RESULT_TYPE_MISMATCH!>++<!>a
}


class Case1() {

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): B {
        TODO()
    }
}

class B() {}

// TESTCASE NUMBER: 2

fun case2() {
    var a = Case2()
    val res: Any? = <!RESULT_TYPE_MISMATCH!>++<!>a
}

class Case2() : C() {
    var i = 0

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): C {
        TODO()
    }

}

open class C() {}
