// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, prefix-expressions, prefix-increment-expression -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check as the result of inc is assigned to A, the return type of inc must be a subtype of A.
 */

// TESTCASE NUMBER: 1

fun case1() {
    var a = Case1()
    val res: Any? = --a
}


class Case1() {

    operator fun dec(): B {
        TODO()
    }
}

class B() {}

// TESTCASE NUMBER: 2

fun case2() {
    var a = Case2()
    val res: Any? = --a
}

class Case2() : C() {
    var i = 0

    operator fun dec(): C {
        TODO()
    }

}

open class C() {}
