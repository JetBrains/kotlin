// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, prefix-expressions, prefix-increment-expression -> paragraph 4 -> sentence 1
 * RELEVANT PLACES: statements, assignments -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check unsafe prefix increment expression call for an assignable expression
 */
class A() {
    var i = 0

    operator fun inc(): A {
        this.i++
        return this
    }
}

// TESTCASE NUMBER: 1

fun case1() {
    var b: Case1? = Case1()
    <!UNSAFE_CALL!>++<!>b?.a
}


class Case1() {
    var a: A = A()
}

// TESTCASE NUMBER: 2

fun case2() {
    var b= Case2()
    ++<!VAL_REASSIGNMENT!>b.a<!>
}

class Case2() {
    val a = A()
}