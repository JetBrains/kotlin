/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and logical expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with complex boolean literals expression in 'when condition'.
fun case_1(value: Boolean): String = when (value) {
    true && false || !!!!true -> ""
    true && !!!true && (!!!false || true) -> ""
}

// CASE DESCRIPTION: 'When' with complex boolean literals expression in 'when condition', and 'else' branch.
fun case_2(value: Boolean): String = when (value) {
    true && false || !!false -> ""
    true && !!!!true && (!!!false || true) -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: 'When' with complex boolean variables expression in 'when condition'.
fun case_3(value: Boolean, value1: Boolean, value2: Boolean, value3: Boolean): String {
    when (value) {
        value1 && value2 || !!!value3 -> return ""
        value2 && !!value1 && (!!!value3 || value1) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with complex boolean variables expression in 'when condition', and 'else' branch.
fun case_4(value: Boolean, value1: Boolean, value2: Boolean, value3: Boolean): String = when (value) {
    value1 && value2 || !!!value3 -> ""
    value2 && !!value1 && (!!!value3 || value1) -> ""
    else -> ""
}
