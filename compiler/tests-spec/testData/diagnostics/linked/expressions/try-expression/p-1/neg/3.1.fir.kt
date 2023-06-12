// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 1 -> sentence 3
 * PRIMARY LINKS: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: try-expression has to start with a try body and continue with zero ore more catch blocks
 */
fun throwException(): Nothing = throw Exception()

class ExcA() : Exception()
class ExcB() : Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try <!SYNTAX!><!>val a = ""
        <!UNRESOLVED_REFERENCE!>catch<!>(<!UNRESOLVED_REFERENCE!>e<!><!SYNTAX!>: Exception<!>) {}
    }<!SYNTAX!><!>

// TESTCASE NUMBER: 2

fun case2() {
    try {
        val a = ""
    } catch (e: Exception)<!SYNTAX!><!>
}

// TESTCASE NUMBER: 3

fun case3() {
    try {
        val a = ""
        throwException()
    } catch (e: java.lang.IllegalArgumentException) {

    } catch (e: ExcB)<!SYNTAX!><!>
}

// TESTCASE NUMBER: 4

fun case4() {
    try {
        throwException()
    } catch (<!SYNTAX!><!>) {

    }
}

// TESTCASE NUMBER: 5

fun case5() {
    try {
        throwException()
    } catch (e: ExcA, <!SYNTAX!>e2<!><!SYNTAX!><!> <!SYNTAX!>: ExcB)<!>
    {}
}


// TESTCASE NUMBER: 6

fun case6() {
    try {
        val a = ""
        throwException()
    } catch (e: java.lang.IllegalArgumentException) {
    } catch (e: ExcA)<!SYNTAX!><!>
    catch (e: ExcB) {
    }
}
