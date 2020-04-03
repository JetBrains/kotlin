// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 1 -> sentence 5
 * RELEVANT PLACES: expressions, try-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: A valid try-expression must have at least one catch or finally block.
 */

fun throwException(): Nothing = throw Exception()

// TESTCASE NUMBER: 1

fun case1() {
    try {
        throwException()
    }<!SYNTAX!><!>
}

// TESTCASE NUMBER: 2

fun case2() {
    try {
        val a = "foo"
    }<!SYNTAX!><!>
}