// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 5
 * NUMBER: 2
 * DESCRIPTION: 'When' with different variants of the arithmetic expressions (additive expression and multiplicative expression) in 'when condition'.
 * HELPERS: typesProvider, classes, functions
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean, value_2: Boolean, value_3: Long) {
    when (value_1) {
        value_2, !value_2, <!CONFUSING_BRANCH_CONDITION_ERROR!>getBoolean() && value_2<!>, <!CONFUSING_BRANCH_CONDITION_ERROR!>getChar() != 'a'<!> -> {}
            <!CONFUSING_BRANCH_CONDITION_ERROR!>getList() === getAny()<!>, <!CONFUSING_BRANCH_CONDITION_ERROR!>value_3 <= 11<!> -> {}
        else -> {}
    }
}

