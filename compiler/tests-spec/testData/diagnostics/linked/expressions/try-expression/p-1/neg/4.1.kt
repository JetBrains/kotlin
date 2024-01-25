// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// COMPARE_WITH_LIGHT_TREE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 1 -> sentence 4
 * PRIMARY LINKS: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: try-expression has to start with a try body, catch blocks and finally block
 */
fun throwException(): Nothing = throw Exception()

class ExcA() : Exception()
class ExcB() : Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    } catch (e: ExcA) {
    } finally {
    } <!UNRESOLVED_REFERENCE!>catch<!> (<!UNRESOLVED_REFERENCE!>e<!><!SYNTAX!><!SYNTAX!><!>: ExcB)<!> <!UNUSED_LAMBDA_EXPRESSION!>{
    }<!>
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        throwException()
    } catch (e: ExcB) {
    } finally
<!SYNTAX!><!>}<!SYNTAX!><!>

// TESTCASE NUMBER: 3

fun case3() {
    try {
        throwException()
    } finally
<!SYNTAX!><!>}<!SYNTAX!><!>


































