// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and with different variants of expressions in the control structure body.
 * HELPERS: typesProvider, classes, functions
 */

// TESTCASE NUMBER: 5
fun case_5(value_1: Int, value_2: Int, value_3: Boolean?) {
    when (value_1) {
        1 -> when (value_3) {
                <!CONFUSING_BRANCH_CONDITION_ERROR!>value_2 > 1000<!> -> "1"
                <!CONFUSING_BRANCH_CONDITION_ERROR!>value_2 > 100<!> -> "2"
            else -> "3"
        }
        2 -> when (value_3) {
                <!CONFUSING_BRANCH_CONDITION_ERROR!>value_2 > 1000<!> -> "1"
                <!CONFUSING_BRANCH_CONDITION_ERROR!>value_2 > 100<!> -> "2"
            else -> ""
        }
        3 -> when (value_3) {
            else -> ""
        }
        4 -> when (value_3) {
            true -> "1"
            false -> "2"
            null -> "3"
            else -> ""
        }
        5 -> when (value_3) {
            true -> "1"
            false -> "2"
            else -> ""
        }
        6 -> when (value_3) {
            else -> ""
        }
    }
}
