// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 11
 * SENTENCE: [8] The bound expression is of a nullable type and one of the areas above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 * NUMBER: 1
 * DESCRIPTION: Checking for not exhaustive 'when' on the nullable Boolean.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    false -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }
