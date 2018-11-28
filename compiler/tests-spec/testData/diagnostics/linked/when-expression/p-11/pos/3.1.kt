/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 11
 * SENTENCE: [3] The bound expression is of type kotlin.Boolean and the conditions contain both:
 * NUMBER: 1
 * DESCRIPTION: Check when exhaustive via boolean bound value and evaluating to value true and false.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): String = when (value_1) {
    true -> ""
    false -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): String = when (value_1) {
    true && false && ((true || false)) || true && !!!false && !!!true -> ""
    true && false && ((true || false)) || true && !!!false -> ""
}
