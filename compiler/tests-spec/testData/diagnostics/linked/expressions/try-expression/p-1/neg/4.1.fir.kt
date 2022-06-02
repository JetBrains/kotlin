// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

fun throwException(): Nothing = throw Exception()

class ExcA() : Exception()
class ExcB() : Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    } catch (e: ExcA) {
    } finally {
    } <!UNRESOLVED_REFERENCE!>catch<!> (<!UNRESOLVED_REFERENCE!>e<!><!SYNTAX!><!SYNTAX!><!>: ExcB)<!> {
    }
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
