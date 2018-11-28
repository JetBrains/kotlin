/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 11
 * SENTENCE: [1] It has an else entry;
 * NUMBER: 1
 * DESCRIPTION: Checking for not exhaustive when without bound value when there is no else branch.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value_1 == 1 -> ""
    value_1 == 2 -> ""
    value_1 == 3 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value_1 == 1 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(): Int = <!NO_ELSE_IN_WHEN!>when<!> {}
