// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check he type of break, continue and return expressions is the Nothing type
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1

fun case1() {
    var name: Any? = null
    val men = arrayListOf(Person("Phill"), Person(), Person("Bob"))
    loop@ for (i in men) {
        i.name
        <!UNREACHABLE_CODE!>val valeua : Int =<!>    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>break@loop<!>
        <!UNREACHABLE_CODE!>i.name<!>
    }
}

class Person(var name: String? = null) {}

// TESTCASE NUMBER: 2

fun case2() {
    var name: Any? = null
    val men = arrayListOf(Person("Phill"), Person(), Person("Bob"))
    loop@ for (i in men) {
        i.name
        <!UNREACHABLE_CODE!>val valeua : Int =<!>    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>continue@loop<!>
        <!UNREACHABLE_CODE!>i.name<!>
    }
}

// TESTCASE NUMBER: 3

fun case3() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        it
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>return@lit<!>
        <!UNREACHABLE_CODE!>print(it)<!>
    }
}