// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNREACHABLE_CODE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, jump-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check he type of jump expressions is the kotlin.Nothing
 */



// TESTCASE NUMBER: 1

fun case1() {
    var name: Any? = null
    val men = arrayListOf(Person("Phill"), Person(), Person("Bob"))
    for (k in men) {
        k.name
        loop@ for (i in men) {
            val val1: Int = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>break@loop<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>val1<!>
        }
        val s = k.name ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>break<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>s<!>
    }
}
class Person(var name: String? = null) {}

// TESTCASE NUMBER: 2

fun case2() {
    var name: Any? = null
    val men = arrayListOf(Person2("Phill"), Person2(), Person2("Bob"))
    for (k in men) {
        loop@ for (i in men) {
            val val1 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>continue@loop<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>val1<!>
        }
        val s = k.name ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>continue<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>s<!>
    }
}

class Person2(var name: String? = null) {}

// TESTCASE NUMBER: 3

fun case3() {
    listOf(1, 2, 3, 4, 5).forEach { x ->
        listOf(1, 2, 3, 4, 5).forEach lit@{
            val s = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>return@lit<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>s<!>
        }
        if (x == 3) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>return<!>
    }
}