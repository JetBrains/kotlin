/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [3] The bound expression is of type kotlin.Boolean and the conditions contain both:
 NUMBER: 1
 DESCRIPTION: Check when exhaustive via boolean bound value and evaluating to value true and false.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (both boolean value covered).
fun case_1(value: Boolean): String = when (value) {
    true -> ""
    false -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (both boolean value as complex expression covered).
fun case_2(value: Boolean): String = when (value) {
    true && false && ((true || false)) || true && !!!false && !!!true -> ""
    true && false && ((true || false)) || true && !!!false -> ""
}
