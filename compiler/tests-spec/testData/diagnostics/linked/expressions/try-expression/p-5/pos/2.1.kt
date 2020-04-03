// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 5 -> sentence 2
 * RELEVANT PLACES: expressions, try-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If an exception was thrown, but no catch block matched its type, the finally block is evaluated before propagating the exception up the call stack.
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-23680
 */
fun case1(): Int {
    var a = 1
    try {
        <!UNREACHABLE_CODE!>throw<!> Exception() //invalid UNREACHABLE_CODE diagnostic
    } catch (e: Exception) {
        a = 5
        <!UNREACHABLE_CODE!>return<!>++a
    } finally {
        return a
    }
    <!UNREACHABLE_CODE!>return 0<!>
}