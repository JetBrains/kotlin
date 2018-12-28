
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and type test condition on the non-type operand of the type checking operator.
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any, <!UNUSED_PARAMETER!>value_2<!>: Int): String {
    when (value_1) {
        is <!UNRESOLVED_REFERENCE!>value_2<!> -> return ""
    }

    return ""
}
