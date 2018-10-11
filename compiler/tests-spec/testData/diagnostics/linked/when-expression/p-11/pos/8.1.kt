/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the areas above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 1
 DESCRIPTION: Check when exhaustive when boolean values are checked and contains a null check.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (both boolean values and null value covered).
fun case_1(value_1: Boolean?): String = when (value_1) {
    true -> ""
    false -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (both boolean values as complex expressions and null value covered).
fun case_2(value_1: Boolean?): String = when (value_1) {
    true && false && ((true || false)) || true && !!!false && !!!true -> ""
    true && false && ((true || false)) || true && !!!false -> ""
    null -> ""
}
