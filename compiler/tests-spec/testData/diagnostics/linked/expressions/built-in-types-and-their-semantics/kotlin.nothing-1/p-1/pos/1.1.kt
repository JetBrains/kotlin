// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check the type of jump expressions is Nothing and code placed on the left side of expression will never be executed
 */

// TESTCASE NUMBER: 1

fun case1() {
    var name: Any? = null
    val men = arrayListOf(Person1("Phill"), Person1(), Person1("Bob"))
    for (k in men) {
        k.name
        loop@ for (i in men) {
            i.name
            <!UNREACHABLE_CODE!>val valeua : Int =<!>     break@loop
            <!UNREACHABLE_CODE!>i.name<!>
        }
        k.name
        val s = k.name ?: break
        k.name
    }
    val a = 1
}

class Person1(var name: String? = null) {}

// TESTCASE NUMBER: 2

fun case2() {
    var name: Any? = null
    val men = arrayListOf(Person2("Phill"), Person2(), Person2("Bob"))
    for (k in men) {
        loop@ for (i in men) {
            i.name
            <!UNREACHABLE_CODE!>val val1 =<!>    continue@loop
            <!UNREACHABLE_CODE!>val1<!>
            <!UNREACHABLE_CODE!>i.name<!>
        }
        val s = k.name ?: continue
        k.name
    }
    val a = 1
}

class Person2(var name: String? = null) {}

// TESTCASE NUMBER: 3

fun case3() {
    listOf(1, 2, 3, 4, 5).forEach { x ->
        val k = x

        listOf(1, 2, 3, 4, 5).forEach lit@{
            it
            return@lit
            <!UNREACHABLE_CODE!>print(it)<!>
        }
        val y = x
        if (x == 3) return
    }
    val a = 1
}
