// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [1] It has an else entry;
 NUMBER: 2
 DESCRIPTION: Checking for not exhaustive when with bound value when there is no else branch.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' (several branches).
fun case_1(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    1 -> ""
    2 -> ""
    3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' (one branch).
fun case_2(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' (no branches).
fun case_3(value_1: Int): Int = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {}
