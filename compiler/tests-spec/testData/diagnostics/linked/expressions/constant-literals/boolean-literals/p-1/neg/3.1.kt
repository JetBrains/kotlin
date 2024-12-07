// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, boolean-literals -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Checking of type for Boolean values
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    true checkType { <!NONE_APPLICABLE!>check<!><Boolean?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Boolean?>() }

    true checkType { <!NONE_APPLICABLE!>check<!><Any?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Any>() }

    true checkType { <!NONE_APPLICABLE!>check<!><Nothing?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Nothing>() }
}