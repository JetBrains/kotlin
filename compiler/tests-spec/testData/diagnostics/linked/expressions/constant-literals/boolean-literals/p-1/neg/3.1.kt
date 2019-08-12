/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, boolean-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Checking of type for Boolean values
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    true checkType { <!TYPE_MISMATCH!>check<!><Boolean?>() }
    false checkType { <!TYPE_MISMATCH!>check<!><Boolean?>() }

    true checkType { <!TYPE_MISMATCH!>check<!><Any?>() }
    false checkType { <!TYPE_MISMATCH!>check<!><Any>() }

    true checkType { <!TYPE_MISMATCH!>check<!><Nothing?>() }
    false checkType { <!TYPE_MISMATCH!>check<!><Nothing>() }
}