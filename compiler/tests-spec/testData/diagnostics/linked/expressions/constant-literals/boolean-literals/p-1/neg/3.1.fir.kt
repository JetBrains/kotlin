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
    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Boolean?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Boolean?>() }

    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Any?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Any>() }

    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Nothing?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Nothing>() }
}
