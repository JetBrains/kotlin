// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 5 -> sentence 1
 * RELEVANT PLACES: expressions, try-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If no exception is thrown during the evaluation of the try body, no catch blocks are executed, the finally block is evaluated after the try body, and the program execution continues as normal.
 */

// TESTCASE NUMBER: 1

fun case1(): String {
    var flag = false
    try {
        flag = true
    } catch (e: Exception) {
       <!UNREACHABLE_CODE!>return<!> "foo"
    } finally {
        return "FINALLY"
    }
    <!UNREACHABLE_CODE!>return "return"<!>
}