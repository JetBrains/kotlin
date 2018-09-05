/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [1] It has an else entry;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when without bound value when there is no else branch.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' (several branches).
fun case_1(value: Int): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value == 1 -> ""
    value == 2 -> ""
    value == 3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' (one branch).
fun case_2(value: Int): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value == 1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' (no branches).
fun case_3(): Int = <!NO_ELSE_IN_WHEN!>when<!> {}
