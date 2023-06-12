// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check unsafe prefix decrement expression call for an assignable expression
 */

// TESTCASE NUMBER: 1

fun case1() {
    var b: Case1? = Case1()
    <!UNSAFE_CALL!>--<!>b?.a
}


class Case1() {
    var a: A = A()
}

class A() {
    var i = 0

    operator fun dec(): A {
        this.i--
        return this
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    var b= Case2()
    --<!VAL_REASSIGNMENT!>b.a<!>
}

class Case2() {
    val a = A2()
}

class A2() {
    var i = 0

    operator fun dec(): A2 {
        this.i--
        return this
    }
}