// FIR_IDENTICAL
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 5
 * NUMBER: 1
 * DESCRIPTION: 'When' with enumeration of the different variants of expressions in 'when condition'.
 * HELPERS: typesProvider, classes, functions
 */

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean, value_2: Boolean, value_3: Long) {
    <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
        value_2 -> {}
        !value_2 -> {}
        <!CONFUSING_BRANCH_CONDITION_ERROR!>getBoolean() && value_2<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_ERROR!>getChar() != 'a'<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_ERROR!>getList() === getAny()<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_ERROR!>value_3 <= 11<!> -> {}
    }
}
