// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check of Unit type
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1

fun foo() {}

fun case1() {
    foo() checkType { check<Unit>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>foo()<!>
}

// TESTCASE NUMBER: 2

fun case2foo(m: String, bar: (m: String) -> Unit) {
    bar(m)
}


class Case2Boo {
    fun buz(m: String) {
        val proc = m
    }
}


fun case2() {
    val boo = Case2Boo()
    val res = case2foo("case 2", boo::buz)
    res checkType { check<Unit>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>res<!>
}

// TESTCASE NUMBER: 3
interface Processable<T> {
    fun process(): T
}

class Processor : Processable<Unit> {
    override fun process() {}
}

fun case3() {
    val p1 = Processor().process()
    p1 checkType { check<Unit>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>p1<!>
}

// TESTCASE NUMBER: 4

fun case4() {
    val p2 = object : Processable<Unit> {
        override fun process() {}
    }.process()
    p2 checkType { check<Unit>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>p2<!>
}

